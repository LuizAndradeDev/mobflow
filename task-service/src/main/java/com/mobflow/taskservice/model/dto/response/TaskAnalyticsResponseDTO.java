package com.mobflow.taskservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAnalyticsResponseDTO {
    private long totalTasks;
    private long completedTasks;
    private long createdTasks;
    private long assignedTasks;
    private long overdueTasks;

    public static TaskAnalyticsResponseDTO fromValues(
            long totalTasks,
            long completedTasks,
            long createdTasks,
            long assignedTasks,
            long overdueTasks
    ) {
        return TaskAnalyticsResponseDTO.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .createdTasks(createdTasks)
                .assignedTasks(assignedTasks)
                .overdueTasks(overdueTasks)
                .build();
    }
}
