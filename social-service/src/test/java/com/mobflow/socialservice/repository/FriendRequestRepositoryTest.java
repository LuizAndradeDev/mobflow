package com.mobflow.socialservice.repository;

import com.mobflow.socialservice.config.MongoConfig;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.testsupport.AbstractMongoSocialTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.REQUESTER_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.TARGET_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.friendRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
class FriendRequestRepositoryTest extends AbstractMongoSocialTest {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @BeforeEach
    void cleanUp() {
        friendRequestRepository.deleteAll();
    }

    @Test
    void findPendingBetweenUsers_pendingRequestExists_returnsPendingRequest() {
        FriendRequest pendingRequest = friendRequest();
        friendRequestRepository.save(pendingRequest);

        Optional<FriendRequest> found = friendRequestRepository.findPendingBetweenUsers(REQUESTER_ID, TARGET_ID);

        assertThat(found).contains(pendingRequest);
    }

    @Test
    void findPendingBetweenUsers_respondedRequest_returnsEmptyOptional() {
        FriendRequest declinedRequest = friendRequest(
                UUID.randomUUID(),
                FriendRequestStatus.DECLINED,
                REQUESTER_ID,
                "john_dev",
                TARGET_ID,
                "mary_dev"
        );
        friendRequestRepository.save(declinedRequest);

        Optional<FriendRequest> found = friendRequestRepository.findPendingBetweenUsers(REQUESTER_ID, TARGET_ID);

        assertThat(found).isEmpty();
    }

    @Test
    void save_respondedRequest_persistsStatusAndResponseTimestamp() {
        FriendRequest acceptedRequest = friendRequest(
                UUID.randomUUID(),
                FriendRequestStatus.ACCEPTED,
                REQUESTER_ID,
                "john_dev",
                TARGET_ID,
                "mary_dev"
        );
        acceptedRequest.setRespondedAt(Instant.parse("2025-01-02T10:00:00Z"));

        FriendRequest savedRequest = friendRequestRepository.save(acceptedRequest);

        assertThat(savedRequest.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        assertThat(savedRequest.getRespondedAt()).isEqualTo(Instant.parse("2025-01-02T10:00:00Z"));
    }

    @Test
    void findAllByParticipant_requesterOrTargetMatches_returnsBothPerspectives() {
        FriendRequest outgoing = friendRequest();
        FriendRequest incoming = friendRequest(
                UUID.randomUUID(),
                FriendRequestStatus.PENDING,
                TARGET_ID,
                "mary_dev",
                REQUESTER_ID,
                "john_dev"
        );

        friendRequestRepository.save(outgoing);
        friendRequestRepository.save(incoming);

        List<FriendRequest> requests = friendRequestRepository.findAllByParticipant(
                REQUESTER_ID,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        assertThat(requests).hasSize(2);
    }
}
