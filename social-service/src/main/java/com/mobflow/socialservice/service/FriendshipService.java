package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.dto.request.SendFriendRequestRequest;
import com.mobflow.socialservice.model.dto.response.FriendRequestResponse;
import com.mobflow.socialservice.model.dto.response.FriendResponse;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.entities.Friendship;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.repository.FriendRequestRepository;
import com.mobflow.socialservice.repository.FriendshipRepository;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendshipService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final AuthServiceClient authServiceClient;
    private final FriendRequestFactory friendRequestFactory;
    private final FriendshipFactory friendshipFactory;
    private final SocialNotificationService socialNotificationService;

    public FriendshipService(
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            AuthServiceClient authServiceClient,
            FriendRequestFactory friendRequestFactory,
            FriendshipFactory friendshipFactory,
            SocialNotificationService socialNotificationService
    ) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.authServiceClient = authServiceClient;
        this.friendRequestFactory = friendRequestFactory;
        this.friendshipFactory = friendshipFactory;
        this.socialNotificationService = socialNotificationService;
    }

    public FriendRequestResponse sendFriendRequest(
            AuthenticatedUser authenticatedUser,
            SendFriendRequestRequest request
    ) {
        AuthServiceClient.AuthUserSummaryResponse targetUser =
                authServiceClient.resolveRequiredByUsername(request.getUsername().trim());

        if (authenticatedUser.authId().equals(targetUser.authId())) {
            throw SocialServiceException.friendRequestToSelf();
        }

        ensureNoPendingRequest(authenticatedUser.authId(), targetUser.authId());
        ensureNoExistingFriendship(authenticatedUser.authId(), authenticatedUser.username(), targetUser.authId(), targetUser.username());

        FriendRequest friendRequest = friendRequestFactory.create(authenticatedUser, targetUser);
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        socialNotificationService.publishFriendRequestSent(savedRequest, authenticatedUser);

        return FriendRequestResponse.fromEntity(savedRequest, authenticatedUser.authId());
    }

    public List<FriendRequestResponse> listFriendRequests(AuthenticatedUser authenticatedUser) {
        return friendRequestRepository.findAllByParticipant(
                        authenticatedUser.authId(),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ).stream()
                .map(request -> FriendRequestResponse.fromEntity(request, authenticatedUser.authId()))
                .toList();
    }

    public FriendRequestResponse acceptFriendRequest(UUID requestId, AuthenticatedUser authenticatedUser) {
        FriendRequest friendRequest = getRequiredFriendRequest(requestId);
        validateRequestOwnership(friendRequest, authenticatedUser.authId());

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw SocialServiceException.invalidFriendRequestState();
        }

        ensureNoExistingFriendship(
                friendRequest.getRequesterId(),
                friendRequest.getRequesterUsername(),
                friendRequest.getTargetId(),
                friendRequest.getTargetUsername()
        );

        Friendship friendship = friendshipFactory.create(friendRequest);
        friendRequest.accept();

        friendshipRepository.save(friendship);
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        socialNotificationService.publishFriendRequestAccepted(savedRequest, authenticatedUser);

        return FriendRequestResponse.fromEntity(savedRequest, authenticatedUser.authId());
    }

    public FriendRequestResponse declineFriendRequest(UUID requestId, AuthenticatedUser authenticatedUser) {
        FriendRequest friendRequest = getRequiredFriendRequest(requestId);
        validateRequestOwnership(friendRequest, authenticatedUser.authId());

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw SocialServiceException.invalidFriendRequestState();
        }

        friendRequest.decline();
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        return FriendRequestResponse.fromEntity(savedRequest, authenticatedUser.authId());
    }

    public List<FriendResponse> listFriends(AuthenticatedUser authenticatedUser) {
        return friendshipRepository.findByUserAOrUserB(authenticatedUser.authId(), authenticatedUser.authId()).stream()
                .map(friendship -> mapFriend(friendship, authenticatedUser.authId()))
                .toList();
    }

    public boolean areFriends(UUID firstAuthId, UUID secondAuthId) {
        FriendshipFactory.NormalizedFriendPair normalizedPair =
                friendshipFactory.normalize(firstAuthId, null, secondAuthId, null);
        return friendshipRepository.findByUserAAndUserB(normalizedPair.userA(), normalizedPair.userB()).isPresent();
    }

    private void ensureNoExistingFriendship(
            UUID firstUserId,
            String firstUsername,
            UUID secondUserId,
            String secondUsername
    ) {
        FriendshipFactory.NormalizedFriendPair normalizedPair =
                friendshipFactory.normalize(firstUserId, firstUsername, secondUserId, secondUsername);

        if (friendshipRepository.findByUserAAndUserB(normalizedPair.userA(), normalizedPair.userB()).isPresent()) {
            throw SocialServiceException.friendshipAlreadyExists();
        }
    }

    private void ensureNoPendingRequest(UUID firstUserId, UUID secondUserId) {
        if (friendRequestRepository.findPendingBetweenUsers(firstUserId, secondUserId).isPresent()) {
            throw SocialServiceException.friendRequestAlreadyExists();
        }
    }

    private FriendRequest getRequiredFriendRequest(UUID requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(SocialServiceException::friendRequestNotFound);
    }

    private void validateRequestOwnership(FriendRequest friendRequest, UUID currentAuthId) {
        if (!friendRequest.getTargetId().equals(currentAuthId)) {
            throw SocialServiceException.accessDenied();
        }
    }

    private FriendResponse mapFriend(Friendship friendship, UUID currentAuthId) {
        if (friendship.getUserA().equals(currentAuthId)) {
            return FriendResponse.of(friendship.getUserB(), friendship.getUserBUsername(), friendship.getCreatedAt());
        }
        return FriendResponse.of(friendship.getUserA(), friendship.getUserAUsername(), friendship.getCreatedAt());
    }
}
