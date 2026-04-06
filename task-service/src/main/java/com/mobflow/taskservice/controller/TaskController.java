package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.request.CreateTaskRequest;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * List all tasks inside a specific list (column).
     */
    @GetMapping("/api/workspaces/{workspaceId}/lists/{listId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> listTasks(
            @PathVariable UUID workspaceId,
            @PathVariable UUID listId
    ) {
        return ResponseEntity.ok(taskService.listTasksByList(listId));
    }

    /**
     * Get a single task by ID.
     */
    @GetMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }

    /**
     * Create a task inside a list.
     */
    @PostMapping("/api/workspaces/{workspaceId}/lists/{listId}/tasks")
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID listId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(workspaceId, listId, authId, request));
    }

    /**
     * Update task fields (title, description, priority, assignee, dueDate, status).
     */
    @PutMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.ok(taskService.updateTask(workspaceId, taskId, authId, request));
    }

    /**
     * Move a task to a different list / position (drag-and-drop).
     * Requires OWNER or ADMIN role.
     */
    @PatchMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/move")
    public ResponseEntity<TaskResponseDTO> moveTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.ok(taskService.moveTask(workspaceId, taskId, authId, request));
    }

    /**
     * Delete a task. Requires OWNER or ADMIN role.
     */
    @DeleteMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        taskService.deleteTask(workspaceId, taskId, authId);
        return ResponseEntity.noContent().build();
    }
}
