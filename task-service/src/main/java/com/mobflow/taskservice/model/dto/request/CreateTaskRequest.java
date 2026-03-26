package com.mobflow.taskservice.model.dto.request;

import com.mobflow.taskservice.model.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 1, max = 255, message = "Task title must be between 1 and 255 characters")
    private String title;

    private String description;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private UUID assigneeAuthId;

    private LocalDate dueDate;
}
