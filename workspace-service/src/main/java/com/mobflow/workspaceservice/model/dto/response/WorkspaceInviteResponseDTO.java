package com.mobflow.workspaceservice.model.dto.response;

import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.enums.InviteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceInviteResponseDTO(
        UUID id,
        UUID workspaceId,
        UUID targetAuthId,
        UUID invitedByAuthId,
        InviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
    public static WorkspaceInviteResponseDTO fromEntity(WorkspaceInvite invite) {
        return new WorkspaceInviteResponseDTO(
                invite.getId(),
                invite.getWorkspace().getId(),
                invite.getTargetAuthId(),
                invite.getInvitedByAuthId(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getRespondedAt()
        );
    }
}
