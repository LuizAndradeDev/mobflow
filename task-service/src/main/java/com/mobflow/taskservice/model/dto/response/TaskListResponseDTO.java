package com.mobflow.taskservice.model.dto.response;

import com.mobflow.taskservice.model.entities.TaskList;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TaskListResponseDTO {

    private UUID id;
    private UUID boardId;
    private String name;
    private int position;
    private List<TaskResponseDTO> tasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskListResponseDTO fromEntity(TaskList list, List<TaskResponseDTO> tasks) {
        return TaskListResponseDTO.builder()
                .id(list.getId())
                .boardId(list.getBoard().getId())
                .name(list.getName())
                .position(list.getPosition())
                .tasks(tasks)
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }

    public static TaskListResponseDTO fromEntityWithoutTasks(TaskList list) {
        return TaskListResponseDTO.builder()
                .id(list.getId())
                .boardId(list.getBoard().getId())
                .name(list.getName())
                .position(list.getPosition())
                .tasks(List.of())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}
