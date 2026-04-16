package com.mobflow.notificationservice.kafka.events;

import java.time.Instant;

public record FriendRequestEvent(
        String eventType,
        String recipientId,
        String actorAuthId,
        String actorUsername,
        String requestId,
        String subjectAuthId,
        String subjectUsername,
        Instant occurredAt
) {
}
