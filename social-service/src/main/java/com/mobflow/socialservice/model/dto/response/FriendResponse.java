package com.mobflow.socialservice.model.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FriendResponse(
        UUID authId,
        String username,
        String avatarUrl,
        Instant friendsSince
) {
    public static FriendResponse of(UUID authId, String username, String avatarUrl, Instant friendsSince) {
        return FriendResponse.builder()
                .authId(authId)
                .username(username)
                .avatarUrl(avatarUrl)
                .friendsSince(friendsSince)
                .build();
    }
}
