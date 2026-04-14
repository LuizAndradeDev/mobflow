package com.mobflow.notificationservice.model.dto.kafka.event;

import java.time.Instant;

public interface DomainEvent {
    String eventType();
    Instant occurredAt();
}
