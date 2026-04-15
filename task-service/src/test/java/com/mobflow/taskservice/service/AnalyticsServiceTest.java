package com.mobflow.taskservice.service;

import com.mobflow.taskservice.model.dto.response.TaskAnalyticsResponseDTO;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository);
    }

    @Test
    void getWorkspaceAnalytics_existingWorkspace_returnsAggregatedCounts() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        when(analyticsRepository.countByWorkspaceId(workspaceId)).thenReturn(10L);
        when(analyticsRepository.countByWorkspaceIdAndCompletedByAuthIdAndStatus(workspaceId, authId, TaskStatus.COMPLETED)).thenReturn(4L);
        when(analyticsRepository.countByWorkspaceIdAndCreatedByAuthId(workspaceId, authId)).thenReturn(6L);
        when(analyticsRepository.countByWorkspaceIdAndAssigneeAuthId(workspaceId, authId)).thenReturn(5L);
        when(analyticsRepository.countOverdueTasksByWorkspaceAndAssignee(eq(workspaceId), eq(authId), eq(LocalDate.now()), eq(TaskStatus.COMPLETED))).thenReturn(2L);

        TaskAnalyticsResponseDTO response = analyticsService.getWorkspaceAnalytics(workspaceId, authId);

        assertThat(response.getTotalTasks()).isEqualTo(10L);
        assertThat(response.getCompletedTasks()).isEqualTo(4L);
        assertThat(response.getCreatedTasks()).isEqualTo(6L);
        assertThat(response.getAssignedTasks()).isEqualTo(5L);
        assertThat(response.getOverdueTasks()).isEqualTo(2L);
    }

    @Test
    void getUserAnalyticsAcrossWorkspaces_existingAssignments_returnsAggregatedCounts() {
        UUID authId = UUID.randomUUID();
        List<UUID> workspaceIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(analyticsRepository.countByWorkspaceIdIn(workspaceIds)).thenReturn(15L);
        when(analyticsRepository.countByWorkspaceIdInAndCompletedByAuthIdAndStatus(workspaceIds, authId, TaskStatus.COMPLETED)).thenReturn(7L);
        when(analyticsRepository.countByWorkspaceIdInAndCreatedByAuthId(workspaceIds, authId)).thenReturn(8L);
        when(analyticsRepository.countByWorkspaceIdInAndAssigneeAuthId(workspaceIds, authId)).thenReturn(9L);
        when(analyticsRepository.countOverdueTasksByWorkspacesAndAssignee(eq(workspaceIds), eq(authId), eq(LocalDate.now()), eq(TaskStatus.COMPLETED))).thenReturn(1L);

        TaskAnalyticsResponseDTO response = analyticsService.getUserAnalyticsAcrossWorkspaces(workspaceIds, authId);

        assertThat(response.getTotalTasks()).isEqualTo(15L);
        assertThat(response.getCompletedTasks()).isEqualTo(7L);
        assertThat(response.getOverdueTasks()).isEqualTo(1L);
    }
}
