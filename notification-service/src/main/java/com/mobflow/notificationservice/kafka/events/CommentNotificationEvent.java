package com.mobflow.notificationservice.kafka.events;

import java.time.Instant;

public record CommentNotificationEvent(
        String eventType,
        String recipientId,
        String actorAuthId,
        String actorUsername,
        String taskId,
        String workspaceId,
        String commentId,
        String taskTitle,
        String commentPreview,
        String mentionedUsername,
        Instant occurredAt
) {
}
