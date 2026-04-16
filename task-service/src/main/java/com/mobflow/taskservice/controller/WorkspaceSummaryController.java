package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.response.WorkspaceSummaryDTO;
import com.mobflow.taskservice.service.WorkspaceSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkspaceSummaryController {

    private final WorkspaceSummaryService workspaceSummaryService;

    public WorkspaceSummaryController(WorkspaceSummaryService workspaceSummaryService) {
        this.workspaceSummaryService = workspaceSummaryService;
    }

    @GetMapping("/api/workspace-summaries/{workspaceId}")
    public ResponseEntity<WorkspaceSummaryDTO> getWorkspaceSummary(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(workspaceSummaryService.getSummary(workspaceId, currentAuthId(authentication)));
    }

    @PostMapping("/api/workspace-summaries/batch")
    public ResponseEntity<List<WorkspaceSummaryDTO>> getBatchSummaries(
            @RequestBody List<UUID> workspaceIds,
            Authentication authentication
    ) {
        return ResponseEntity.ok(workspaceSummaryService.getSummaries(workspaceIds, currentAuthId(authentication)));
    }

    private UUID currentAuthId(Authentication authentication) {
        return (UUID) authentication.getCredentials();
    }
}
