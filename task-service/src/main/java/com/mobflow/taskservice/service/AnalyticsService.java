package com.mobflow.taskservice.service;

import com.mobflow.taskservice.model.dto.response.TaskAnalyticsResponseDTO;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.util.Collection;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository
    ) {
        this.analyticsRepository = analyticsRepository;
    }


    public TaskAnalyticsResponseDTO getWorkspaceAnalytics(UUID workspaceId, UUID authId) {

        long totalTasks = analyticsRepository.countByWorkspaceId(workspaceId);

        long completedTasks = analyticsRepository.countByWorkspaceIdAndCompletedByAuthIdAndStatus(
                workspaceId,
                authId,
                TaskStatus.COMPLETED
        );

        long createdTasks = analyticsRepository.countByWorkspaceIdAndCreatedByAuthId(
                workspaceId,
                authId
        );

        long assignedTasks = analyticsRepository.countByWorkspaceIdAndAssigneeAuthId(
                workspaceId,
                authId
        );

        long overdueTasks = analyticsRepository.countOverdueTasksByWorkspaceAndAssignee(
                workspaceId,
                authId,
                LocalDate.now(),
                TaskStatus.COMPLETED
        );

        return TaskAnalyticsResponseDTO.fromValues(
                totalTasks,
                completedTasks,
                createdTasks,
                assignedTasks,
                overdueTasks
        );
    }

    public TaskAnalyticsResponseDTO getUserAnalyticsAcrossWorkspaces(
            Collection<UUID> workspaceIds,
            UUID authId
    ) {

        long totalTasks = analyticsRepository.countByWorkspaceIdIn(workspaceIds);

        long completedTasks = analyticsRepository.countByWorkspaceIdInAndCompletedByAuthIdAndStatus(
                workspaceIds,
                authId,
                TaskStatus.COMPLETED
        );

        long createdTasks = analyticsRepository.countByWorkspaceIdInAndCreatedByAuthId(
                workspaceIds,
                authId
        );

        long assignedTasks = analyticsRepository.countByWorkspaceIdInAndAssigneeAuthId(
                workspaceIds,
                authId
        );

        long overdueTasks = analyticsRepository.countOverdueTasksByWorkspacesAndAssignee(
                workspaceIds,
                authId,
                LocalDate.now(),
                TaskStatus.COMPLETED
        );

        return TaskAnalyticsResponseDTO.fromValues(
                totalTasks,
                completedTasks,
                createdTasks,
                assignedTasks,
                overdueTasks
        );
    }

    public TaskAnalyticsResponseDTO getUserAnalytics(
            UUID authId
    ) {

        long completedTasks = analyticsRepository.countByCompletedByAuthId(authId);

        long createdTasks = analyticsRepository.countByCreatedByAuthId(authId);

        long assignedTasks = analyticsRepository.countByAssigneeAuthId(authId);

        long overdueTasks = analyticsRepository.countOverdueTasksByAssignee(
                authId,
                LocalDate.now(),
                TaskStatus.COMPLETED
        );

        return TaskAnalyticsResponseDTO.fromValues(
                0,
                completedTasks,
                createdTasks,
                assignedTasks,
                overdueTasks
        );
    }
}
