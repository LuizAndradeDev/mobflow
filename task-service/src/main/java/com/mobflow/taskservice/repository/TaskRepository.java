package com.mobflow.taskservice.repository;

import com.mobflow.taskservice.model.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByListIdOrderByPositionAsc(UUID listId);

    List<Task> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId AND t.dueDate BETWEEN :from AND :to ORDER BY t.dueDate ASC")
    List<Task> findByWorkspaceIdAndDueDateBetween(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT t FROM Task t WHERE t.assigneeAuthId = :authId AND t.workspaceId IN :workspaceIds ORDER BY t.dueDate ASC NULLS LAST")
    List<Task> findByAssigneeAndWorkspaces(
            @Param("authId") UUID authId,
            @Param("workspaceIds") List<UUID> workspaceIds
    );

    int countByListId(UUID listId);

    List<Task> findByDueDateAndStatusNot(LocalDate dueDate, com.mobflow.taskservice.model.enums.TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.workspaceId IN :workspaceIds ORDER BY t.createdAt DESC")
    List<Task> findByWorkspaceIds(@Param("workspaceIds") List<UUID> workspaceIds);
}
