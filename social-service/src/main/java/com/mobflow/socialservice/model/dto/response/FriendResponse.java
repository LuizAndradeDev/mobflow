package com.mobflow.socialservice.model.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FriendResponse(
        UUID authId,
        String username,
        Instant friendsSince
) {
    public static FriendResponse of(UUID authId, String username, Instant friendsSince) {
        return FriendResponse.builder()
                .authId(authId)
                .username(username)
                .friendsSince(friendsSince)
                .build();
    }
}
