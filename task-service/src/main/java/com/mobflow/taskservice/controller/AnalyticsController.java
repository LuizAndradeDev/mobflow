package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.response.TaskAnalyticsResponseDTO;
import com.mobflow.taskservice.service.AnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/task-analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService){
        this.analyticsService = analyticsService;
    }

    @GetMapping("/workspace/{workspaceId}/user/{authId}")
    public TaskAnalyticsResponseDTO getWorkspaceAnalytics(
            @PathVariable UUID workspaceId,
            @PathVariable UUID authId,
            Authentication authentication
    ) {
        UUID callerAuthId = (UUID) authentication.getCredentials();
        if (!callerAuthId.equals(authId)) throw TaskServiceException.accessDenied();
        return analyticsService.getWorkspaceAnalytics(workspaceId, authId);
    }

    @PostMapping("/user/{authId}/workspaces")
    public TaskAnalyticsResponseDTO getUserAnalyticsAcrossWorkspaces(
            @PathVariable UUID authId,
            @RequestBody Collection<UUID> workspaceIds,
            Authentication authentication
    ) {
        UUID callerAuthId = (UUID) authentication.getCredentials();
        if (!callerAuthId.equals(authId)) throw TaskServiceException.accessDenied();
        return analyticsService.getUserAnalyticsAcrossWorkspaces(workspaceIds, authId);
    }

    @GetMapping("/user/{authId}")
    public TaskAnalyticsResponseDTO getUserAnalytics(
            @PathVariable UUID authId,
            Authentication authentication
    ) {
        UUID callerAuthId = (UUID) authentication.getCredentials();
        if (!callerAuthId.equals(authId)) throw TaskServiceException.accessDenied();
        return analyticsService.getUserAnalytics(authId);
    }
}
