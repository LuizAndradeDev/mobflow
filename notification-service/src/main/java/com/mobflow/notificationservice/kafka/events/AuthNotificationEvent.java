package com.mobflow.notificationservice.kafka.events;

import java.time.Instant;

public record AuthNotificationEvent(
        String eventType,
        String recipientId,
        String recipientEmail,
        String recipientUsername,
        String confirmationToken,
        String confirmationUrl,
        Instant occurredAt
) {
}
