package com.mobflow.notificationservice.model.dto.response;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;

import java.time.Instant;
import java.util.Map;

public record NotificationResponseDTO(
        String id,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String body,
        NotificationPriority priority,
        boolean read,
        Instant createdAt,
        Instant readAt,
        Map<String, String> metadata
) {
    public static NotificationResponseDTO fromEntity(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getType(),
                notification.getChannel(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPriority(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getMetadata()
        );
    }
}
