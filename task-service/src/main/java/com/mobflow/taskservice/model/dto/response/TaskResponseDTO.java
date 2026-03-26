package com.mobflow.taskservice.model.dto.response;

import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.enums.TaskPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponseDTO {

    private UUID id;
    private UUID listId;
    private UUID workspaceId;
    private String title;
    private String description;
    private TaskPriority priority;
    private UUID assigneeAuthId;
    private String assigneeDisplayName;
    private String assigneeAvatarUrl;
    private UUID createdByAuthId;
    private LocalDate dueDate;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponseDTO fromEntity(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .listId(task.getList().getId())
                .workspaceId(task.getWorkspaceId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .assigneeAuthId(task.getAssigneeAuthId())
                .createdByAuthId(task.getCreatedByAuthId())
                .dueDate(task.getDueDate())
                .position(task.getPosition())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static TaskResponseDTO fromEntity(Task task, String assigneeDisplayName, String assigneeAvatarUrl) {
        TaskResponseDTO dto = fromEntity(task);
        dto.setAssigneeDisplayName(assigneeDisplayName);
        dto.setAssigneeAvatarUrl(assigneeAvatarUrl);
        return dto;
    }
}
