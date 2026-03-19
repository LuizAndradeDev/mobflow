package com.mobflow.workspaceservice.repository;

import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndAuthId(UUID workspaceId, UUID authId);

    boolean existsByWorkspaceIdAndAuthId(UUID workspaceId, UUID authId);
}
