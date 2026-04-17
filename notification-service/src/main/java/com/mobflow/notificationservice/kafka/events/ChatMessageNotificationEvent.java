package com.mobflow.notificationservice.kafka.events;

import java.time.Instant;

public record ChatMessageNotificationEvent(
        String eventType,
        String messageId,
        String conversationId,
        String senderId,
        String recipientId,
        String contentPreview,
        Instant createdAt
) {
}
