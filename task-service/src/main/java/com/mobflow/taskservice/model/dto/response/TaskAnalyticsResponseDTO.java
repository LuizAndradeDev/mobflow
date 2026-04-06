package com.mobflow.taskservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAnalyticsResponse {
    private long totalTasks;
    private long completedTasks;
    private long createdTasks;
    private long assignedTasks;
    
}
