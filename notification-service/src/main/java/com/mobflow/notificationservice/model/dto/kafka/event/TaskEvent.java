package com.mobflow.notificationservice.model.dto.kafka.event;

import java.time.Instant;

public record TaskEvent(
        String eventType,
        String authorDisplayName,
        String title,
        String workspaceId,
        String workspaceName,
        String assigneeAuthId,
        String assignedByDisplayName,
        Instant occurredAt
) implements DomainEvent
{}


