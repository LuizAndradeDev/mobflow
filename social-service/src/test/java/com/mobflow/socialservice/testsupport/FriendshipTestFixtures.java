package com.mobflow.socialservice.testsupport;

import com.mobflow.socialservice.model.entities.Friendship;

import java.time.Instant;
import java.util.UUID;

public final class FriendshipTestFixtures {

    private FriendshipTestFixtures() {
    }

    public static Friendship friendship() {
        return friendship(
                UUID.randomUUID(),
                FriendRequestTestFixtures.REQUESTER_ID,
                "john_dev",
                FriendRequestTestFixtures.TARGET_ID,
                "mary_dev"
        );
    }

    public static Friendship friendship(
            UUID id,
            UUID userA,
            String userAUsername,
            UUID userB,
            String userBUsername
    ) {
        return Friendship.builder()
                .id(id)
                .userA(userA)
                .userAUsername(userAUsername)
                .userB(userB)
                .userBUsername(userBUsername)
                .createdAt(Instant.now())
                .build();
    }
}
