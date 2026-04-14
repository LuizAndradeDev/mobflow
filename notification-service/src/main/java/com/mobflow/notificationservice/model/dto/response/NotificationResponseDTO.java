package com.mobflow.notificationservice.model.dto.response;

import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;

import java.time.Instant;
import java.util.Map;

public record NotificationResponseDTO(
        String id,
        NotificationType type,
        NotificationPriority priority,
        boolean read,
        Instant createdAt,
        Instant readAt,
        Map<String, String> metadata
){}
