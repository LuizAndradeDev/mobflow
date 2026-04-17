package com.mobflow.socialservice.kafka.events;

import java.time.Instant;

public record CommentNotificationEvent(
        String eventType,
        String recipientId,
        String actorAuthId,
        String actorUsername,
        String taskId,
        String workspaceId,
        String boardId,
        String commentId,
        String taskTitle,
        String commentPreview,
        String mentionedUsername,
        Instant occurredAt
) {
}
