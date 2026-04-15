package com.mobflow.workspaceservice.repository;

import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {
    boolean existsByWorkspaceIdAndTargetAuthIdAndStatus(UUID workspaceId, UUID targetAuthId, InviteStatus status);
    Optional<WorkspaceInvite> findByIdAndTargetAuthId(UUID inviteId, UUID targetAuthId);
}
