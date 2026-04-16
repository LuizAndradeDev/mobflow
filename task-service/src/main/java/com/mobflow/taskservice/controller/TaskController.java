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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/workspaces/{workspaceId}/lists/{listId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> listTasks(
            @PathVariable UUID workspaceId,
            @PathVariable UUID listId
    ) {
        return ResponseEntity.ok(taskService.listTasksByList(listId));
    }

    @GetMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> getTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId
    ) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }

    @PostMapping("/api/workspaces/{workspaceId}/lists/{listId}/tasks")
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID listId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(workspaceId, listId, currentAuthId(authentication), request));
    }

    @PutMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.updateTask(workspaceId, taskId, currentAuthId(authentication), request));
    }

    @PatchMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/move")
    public ResponseEntity<TaskResponseDTO> moveTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.moveTask(workspaceId, taskId, currentAuthId(authentication), request));
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        taskService.deleteTask(workspaceId, taskId, currentAuthId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID currentAuthId(Authentication authentication) {
        return (UUID) authentication.getCredentials();
    }
}
