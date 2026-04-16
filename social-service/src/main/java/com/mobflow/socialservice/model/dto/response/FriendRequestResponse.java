package com.mobflow.socialservice.model.dto.response;

import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record FriendRequestResponse(
        UUID id,
        UUID requesterId,
        String requesterUsername,
        UUID targetId,
        String targetUsername,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        boolean incoming
) {
    public static FriendRequestResponse fromEntity(FriendRequest friendRequest, UUID currentAuthId) {
        return FriendRequestResponse.builder()
                .id(friendRequest.getId())
                .requesterId(friendRequest.getRequesterId())
                .requesterUsername(friendRequest.getRequesterUsername())
                .targetId(friendRequest.getTargetId())
                .targetUsername(friendRequest.getTargetUsername())
                .status(friendRequest.getStatus())
                .createdAt(friendRequest.getCreatedAt())
                .respondedAt(friendRequest.getRespondedAt())
                .incoming(friendRequest.getTargetId().equals(currentAuthId))
                .build();
    }
}
