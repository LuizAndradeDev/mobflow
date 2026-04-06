package com.mobflow.taskservice.repository;

import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public interface AnalyticsRepository extends JpaRepository<Task, UUID> {
    long countByWorkspaceId(UUID workspaceId);

    long countByCreatedByAuthId(UUID createdByAuthId);

    long countByCompletedByAuthId(UUID completedByAuthId);

    long countByAssigneeAuthId(UUID assigneeAuthId);

    long countByWorkspaceIdAndCreatedByAuthId(UUID workspaceId, UUID createdByAuthId);

    long countByWorkspaceIdAndAssigneeAuthId(UUID workspaceId, UUID assigneeAuthId);

    long countByWorkspaceIdAndCompletedByAuthIdAndStatus(UUID workspaceId, UUID completedByAuthId, TaskStatus status);

    long countByWorkspaceIdInAndCreatedByAuthId(Collection<UUID> workspaceIds, UUID createdByAuthId);

    long countByWorkspaceIdInAndAssigneeAuthId(Collection<UUID> workspaceIds, UUID assigneeAuthId);

    long countByWorkspaceIdIn(Collection<UUID> workspaceIds);

    long countByWorkspaceIdInAndCompletedByAuthIdAndStatus(Collection<UUID> workspaceIds, UUID completedByAuthId, TaskStatus status);

    @Query("""
            SELECT COUNT(t)
            FROM Task t
            WHERE t.workspaceId = :workspaceId
              AND t.assigneeAuthId = :authId
              AND t.dueDate < :now
              AND t.status <> :doneStatus
            """)
    long countOverdueTasksByWorkspaceAndAssignee(UUID workspaceId,
                                                 UUID authId,
                                                 LocalDate now,
                                                 TaskStatus doneStatus);

    @Query("""
            SELECT COUNT(t)
            FROM Task t
            WHERE t.workspaceId IN :workspaceIds
              AND t.assigneeAuthId = :authId
              AND t.dueDate < :now
              AND t.status <> :doneStatus
            """)
    long countOverdueTasksByWorkspacesAndAssignee(Collection<UUID> workspaceIds,
                                                  UUID authId,
                                                  LocalDate now,
                                                  TaskStatus doneStatus);

    @Query("""
            SELECT COUNT(t)
            FROM Task t
            WHERE t.assigneeAuthId = :authId
              AND t.dueDate < :now
              AND t.status <> :doneStatus
            """)
    long countOverdueTasksByAssignee(UUID authId,
                                     LocalDate now,
                                     TaskStatus doneStatus);

}
