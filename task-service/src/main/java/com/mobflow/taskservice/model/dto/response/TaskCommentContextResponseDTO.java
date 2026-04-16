package com.mobflow.taskservice.model.dto.response;

import com.mobflow.taskservice.model.entities.Task;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskCommentContextResponseDTO(
        UUID taskId,
        UUID workspaceId,
        UUID createdByAuthId,
        UUID assigneeAuthId,
        String taskTitle
) {
    public static TaskCommentContextResponseDTO fromEntity(Task task) {
        return TaskCommentContextResponseDTO.builder()
                .taskId(task.getId())
                .workspaceId(task.getWorkspaceId())
                .createdByAuthId(task.getCreatedByAuthId())
                .assigneeAuthId(task.getAssigneeAuthId())
                .taskTitle(task.getTitle())
                .build();
    }
}
