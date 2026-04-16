package com.mobflow.socialservice.service;

import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.entities.Friendship;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FriendshipFactory {

    public Friendship create(FriendRequest friendRequest) {
        NormalizedFriendPair normalizedPair = normalize(
                friendRequest.getRequesterId(),
                friendRequest.getRequesterUsername(),
                friendRequest.getTargetId(),
                friendRequest.getTargetUsername()
        );

        return Friendship.create(
                normalizedPair.userA(),
                normalizedPair.userAUsername(),
                normalizedPair.userB(),
                normalizedPair.userBUsername()
        );
    }

    public NormalizedFriendPair normalize(
            UUID firstUserId,
            String firstUsername,
            UUID secondUserId,
            String secondUsername
    ) {
        if (firstUserId.compareTo(secondUserId) <= 0) {
            return new NormalizedFriendPair(firstUserId, firstUsername, secondUserId, secondUsername);
        }
        return new NormalizedFriendPair(secondUserId, secondUsername, firstUserId, firstUsername);
    }

    public record NormalizedFriendPair(
            UUID userA,
            String userAUsername,
            UUID userB,
            String userBUsername
    ) {
    }
}
