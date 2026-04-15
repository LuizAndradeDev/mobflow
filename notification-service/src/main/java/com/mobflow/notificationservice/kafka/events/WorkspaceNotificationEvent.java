package com.mobflow.notificationservice.kafka.events;

import java.time.Instant;

public record WorkspaceNotificationEvent(
        String eventType,
        String recipientId,
        String recipientEmail,
        String recipientDisplayName,
        String actorAuthId,
        String actorDisplayName,
        String workspaceId,
        String workspaceName,
        String inviteId,
        String role,
        Instant occurredAt
) {
}
