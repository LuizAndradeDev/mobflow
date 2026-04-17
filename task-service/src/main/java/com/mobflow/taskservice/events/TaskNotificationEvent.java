package com.mobflow.taskservice.events;

import java.time.Instant;
import java.time.LocalDate;

public record TaskNotificationEvent(
        String eventType,
        String recipientId,
        String recipientEmail,
        String recipientDisplayName,
        String actorAuthId,
        String actorDisplayName,
        String workspaceId,
        String boardId,
        String workspaceName,
        String taskId,
        String taskTitle,
        String taskStatus,
        LocalDate dueDate,
        Instant occurredAt
) {
}
