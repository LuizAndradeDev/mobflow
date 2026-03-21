package com.mobflow.workspaceservice.repository;

import com.mobflow.workspaceservice.model.entities.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("""
            SELECT w FROM Workspace w
            WHERE w.id IN (
                SELECT m.workspace.id FROM WorkspaceMember m WHERE m.authId = :authId
            )
            ORDER BY w.createdAt DESC
            """)
    List<Workspace> findAllByMemberAuthId(@Param("authId") UUID authId);

    Optional<Workspace> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);
}
