package com.mobflow.socialservice.testsupport;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.model.dto.request.SendFriendRequestRequest;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.security.AuthenticatedUser;

import java.time.Instant;
import java.util.UUID;

public final class FriendRequestTestFixtures {

    public static final UUID REQUESTER_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID TARGET_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");

    private FriendRequestTestFixtures() {
    }

    public static AuthenticatedUser requester() {
        return new AuthenticatedUser(REQUESTER_ID, "john_dev");
    }

    public static AuthenticatedUser targetUser() {
        return new AuthenticatedUser(TARGET_ID, "mary_dev");
    }

    public static SendFriendRequestRequest sendFriendRequestRequest(String username) {
        SendFriendRequestRequest request = new SendFriendRequestRequest();
        request.setUsername(username);
        return request;
    }

    public static AuthServiceClient.AuthUserSummaryResponse resolvedTarget() {
        return new AuthServiceClient.AuthUserSummaryResponse(TARGET_ID, "mary_dev");
    }

    public static FriendRequest friendRequest() {
        return friendRequest(UUID.randomUUID(), FriendRequestStatus.PENDING, REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev");
    }

    public static FriendRequest friendRequest(
            UUID id,
            FriendRequestStatus status,
            UUID requesterId,
            String requesterUsername,
            UUID targetId,
            String targetUsername
    ) {
        return FriendRequest.builder()
                .id(id)
                .requesterId(requesterId)
                .requesterUsername(requesterUsername)
                .targetId(targetId)
                .targetUsername(targetUsername)
                .status(status)
                .createdAt(Instant.now())
                .respondedAt(status == FriendRequestStatus.PENDING ? null : Instant.now())
                .build();
    }
}
