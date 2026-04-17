package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.client.UserServiceClient;
import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.dto.response.FriendRequestResponse;
import com.mobflow.socialservice.model.dto.response.FriendResponse;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.entities.Friendship;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.repository.FriendRequestRepository;
import com.mobflow.socialservice.repository.FriendshipRepository;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.REQUESTER_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.TARGET_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.friendRequest;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.requester;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.resolvedTarget;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.sendFriendRequestRequest;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.targetUser;
import static com.mobflow.socialservice.testsupport.FriendshipTestFixtures.friendship;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private FriendRequestFactory friendRequestFactory;

    @Mock
    private FriendshipFactory friendshipFactory;

    @Mock
    private SocialNotificationService socialNotificationService;

    @InjectMocks
    private FriendshipService friendshipService;

    private AuthenticatedUser requester;

    @BeforeEach
    void setUp() {
        requester = requester();
        FriendshipFactory defaultFactory = new FriendshipFactory();
        lenient().when(friendshipFactory.normalize(any(UUID.class), any(), any(UUID.class), any()))
                .thenAnswer(invocation -> defaultFactory.normalize(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)
                ));
    }

    @Test
    void sendFriendRequest_validRequest_returnsCreatedRequest() {
        FriendRequest unsavedRequest = friendRequest();
        FriendRequest savedRequest = friendRequest();

        when(authServiceClient.resolveRequiredByUsername("mary_dev")).thenReturn(resolvedTarget());
        when(friendRequestRepository.findPendingBetweenUsers(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.empty());
        when(friendshipRepository.findByUserAAndUserB(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.empty());
        when(friendRequestFactory.create(requester, resolvedTarget())).thenReturn(unsavedRequest);
        when(friendRequestRepository.save(unsavedRequest)).thenReturn(savedRequest);

        FriendRequestResponse response = friendshipService.sendFriendRequest(requester, sendFriendRequestRequest("mary_dev"));

        assertThat(response.requesterId()).isEqualTo(REQUESTER_ID);
        assertThat(response.targetId()).isEqualTo(TARGET_ID);
        verify(socialNotificationService).publishFriendRequestSent(savedRequest, requester);
    }

    @Test
    void sendFriendRequest_selfRequest_throwsBusinessException() {
        when(authServiceClient.resolveRequiredByUsername("john_dev"))
                .thenReturn(new AuthServiceClient.AuthUserSummaryResponse(REQUESTER_ID, "john_dev"));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester, sendFriendRequestRequest("john_dev")))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("You cannot send a friend request to yourself");
    }

    @Test
    void sendFriendRequest_pendingRequestExists_throwsConflict() {
        when(authServiceClient.resolveRequiredByUsername("mary_dev")).thenReturn(resolvedTarget());
        when(friendRequestRepository.findPendingBetweenUsers(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.of(friendRequest()));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester, sendFriendRequestRequest("mary_dev")))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("A pending friend request already exists for these users");

        verify(friendshipRepository, never()).findByUserAAndUserB(any(), any());
    }

    @Test
    void sendFriendRequest_existingFriendship_throwsConflict() {
        when(authServiceClient.resolveRequiredByUsername("mary_dev")).thenReturn(resolvedTarget());
        when(friendRequestRepository.findPendingBetweenUsers(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.empty());
        when(friendshipRepository.findByUserAAndUserB(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.of(friendship()));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester, sendFriendRequestRequest("mary_dev")))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("Friendship already exists");
    }

    @Test
    void acceptFriendRequest_pendingRequest_returnsAcceptedResponse() {
        UUID requestId = UUID.randomUUID();
        AuthenticatedUser target = targetUser();
        FriendRequest pendingRequest = friendRequest(requestId, FriendRequestStatus.PENDING, REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");
        Friendship createdFriendship = friendship();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(friendshipRepository.findByUserAAndUserB(REQUESTER_ID, TARGET_ID)).thenReturn(Optional.empty());
        when(friendshipFactory.create(pendingRequest)).thenReturn(createdFriendship);
        when(friendshipRepository.save(createdFriendship)).thenReturn(createdFriendship);
        when(friendRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        FriendRequestResponse response = friendshipService.acceptFriendRequest(requestId, target);

        assertThat(response.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(socialNotificationService).publishFriendRequestAccepted(pendingRequest, target);
    }

    @Test
    void declineFriendRequest_pendingRequest_returnsDeclinedResponse() {
        UUID requestId = UUID.randomUUID();
        AuthenticatedUser target = targetUser();
        FriendRequest pendingRequest = friendRequest(requestId, FriendRequestStatus.PENDING, REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        FriendRequestResponse response = friendshipService.declineFriendRequest(requestId, target);

        assertThat(response.status()).isEqualTo(FriendRequestStatus.DECLINED);
        verify(socialNotificationService, never()).publishFriendRequestAccepted(any(), any());
    }

    @Test
    void acceptFriendRequest_wrongTargetUser_throwsAccessDenied() {
        UUID requestId = UUID.randomUUID();
        AuthenticatedUser anotherUser = new AuthenticatedUser(UUID.randomUUID(), "other");
        FriendRequest pendingRequest = friendRequest(requestId, FriendRequestStatus.PENDING, REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(requestId, anotherUser))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("You are not allowed to perform this action");
    }

    @Test
    void acceptFriendRequest_alreadyResponded_throwsInvalidState() {
        UUID requestId = UUID.randomUUID();
        AuthenticatedUser target = targetUser();
        FriendRequest acceptedRequest = friendRequest(requestId, FriendRequestStatus.ACCEPTED, REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(acceptedRequest));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(requestId, target))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("Friend request cannot be processed in its current state");
    }

    @Test
    void listFriendRequests_existingRequests_returnsSortedMappedResponses() {
        FriendRequest first = friendRequest();
        FriendRequest second = friendRequest(UUID.randomUUID(), FriendRequestStatus.PENDING, TARGET_ID, "mary_dev", REQUESTER_ID, "john_dev");

        when(friendRequestRepository.findAllByParticipant(eq(REQUESTER_ID), any(Sort.class)))
                .thenReturn(List.of(first, second));

        List<FriendRequestResponse> responses = friendshipService.listFriendRequests(requester);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(FriendRequestResponse::incoming).containsExactly(false, true);
    }

    @Test
    void listFriends_existingFriendships_returnsOtherSideAsFriendResponse() {
        Friendship friendship = friendship(UUID.randomUUID(), REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");
        when(friendshipRepository.findByUserAOrUserB(REQUESTER_ID, REQUESTER_ID)).thenReturn(List.of(friendship));
        when(userServiceClient.fetchProfiles(List.of(TARGET_ID))).thenReturn(List.of(
                new UserServiceClient.UserProfileResponse(TARGET_ID, "Mary", "http://cdn.mobflow.dev/mary.png")
        ));

        List<FriendResponse> responses = friendshipService.listFriends(requester);

        assertThat(responses).singleElement()
                .extracting(FriendResponse::authId, FriendResponse::username, FriendResponse::avatarUrl)
                .containsExactly(TARGET_ID, "mary_dev", "http://cdn.mobflow.dev/mary.png");
    }
}
