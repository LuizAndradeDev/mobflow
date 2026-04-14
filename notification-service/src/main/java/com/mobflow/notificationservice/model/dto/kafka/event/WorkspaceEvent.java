package com.mobflow.notificationservice.model.dto.kafka.event;

import java.time.Instant;

public record WorkspaceEvent(
        Instant occurredAt,
        String eventType,
        String authorDisplayName,
        String subjetctDisplayName,
        String workspaceId,
        String workspaceName,
        String assigneeAuthId,
        String assignedByDisplayName
)
        implements DomainEvent {}
