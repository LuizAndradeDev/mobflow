package com.mobflow.taskservice.repository;

import com.mobflow.taskservice.model.entities.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByWorkspaceIdOrderByPositionAsc(UUID workspaceId);

    Optional<Board> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    int countByWorkspaceId(UUID workspaceId);
}
