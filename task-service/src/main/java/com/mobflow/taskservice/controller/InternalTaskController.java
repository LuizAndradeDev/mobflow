package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.response.WorkspaceSummaryDTO;
import com.mobflow.taskservice.service.WorkspaceSummaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/tasks")
public class InternalTaskController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final WorkspaceSummaryService workspaceSummaryService;
    private final String internalSecret;

    public InternalTaskController(
            WorkspaceSummaryService workspaceSummaryService,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.workspaceSummaryService = workspaceSummaryService;
        this.internalSecret = internalSecret;
    }

    /**
     * Returns the compact board+task overview for a single workspace.
     * Called by the frontend Angular service directly (no auth required on /internal).
     * Protected by the shared internal secret header.
     */
    @GetMapping("/summary/{workspaceId}")
    public ResponseEntity<WorkspaceSummaryDTO> getWorkspaceSummary(
            @PathVariable UUID workspaceId,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(workspaceSummaryService.getSummary(workspaceId));
    }

    /**
     * Batch version — receives a list of workspaceIds and returns summaries for all.
     * Used by the /tasks overview page to load all user workspaces in one call.
     */
    @PostMapping("/summaries")
    public ResponseEntity<List<WorkspaceSummaryDTO>> getBatchSummaries(
            @RequestBody List<UUID> workspaceIds,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(workspaceSummaryService.getSummaries(workspaceIds));
    }
}
