package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.response.WorkspaceSummaryDTO;
import com.mobflow.taskservice.service.WorkspaceSummaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/summary/{workspaceId}")
    public ResponseEntity<WorkspaceSummaryDTO> getWorkspaceSummary(
            @PathVariable UUID workspaceId,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(workspaceSummaryService.getSummary(workspaceId));
    }

    @PostMapping("/summaries")
    public ResponseEntity<List<WorkspaceSummaryDTO>> getBatchSummaries(
            @RequestBody List<UUID> workspaceIds,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(workspaceSummaryService.getSummaries(workspaceIds));
    }

    private boolean hasValidSecret(String secret) {
        return internalSecret.equals(secret);
    }
}
