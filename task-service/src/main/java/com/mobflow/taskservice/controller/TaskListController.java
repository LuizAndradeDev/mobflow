package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.request.CreateTaskListRequest;
import com.mobflow.taskservice.model.dto.request.ReorderListsRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskListRequest;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.service.TaskListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/boards/{boardId}/lists")
public class TaskListController {

    private final TaskListService taskListService;

    public TaskListController(TaskListService taskListService) {
        this.taskListService = taskListService;
    }

    @PostMapping
    public ResponseEntity<TaskListResponseDTO> createList(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            @Valid @RequestBody CreateTaskListRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskListService.createList(workspaceId, boardId, authId, request));
    }

    @PutMapping("/{listId}")
    public ResponseEntity<TaskListResponseDTO> updateList(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            @PathVariable UUID listId,
            @Valid @RequestBody UpdateTaskListRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.ok(taskListService.updateList(workspaceId, boardId, listId, authId, request));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            @PathVariable UUID listId,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        taskListService.deleteList(workspaceId, boardId, listId, authId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderLists(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            @Valid @RequestBody ReorderListsRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        taskListService.reorderLists(workspaceId, boardId, authId, request);
        return ResponseEntity.noContent().build();
    }
}
