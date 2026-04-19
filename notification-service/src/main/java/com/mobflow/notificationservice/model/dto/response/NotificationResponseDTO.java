package com.mobflow.notificationservice.model.dto.response;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;

import java.time.Instant;
import java.util.Map;

public record NotificationResponseDTO(
        String id,
        String recipientId,
        String recipientEmail,
        NotificationType type,
        NotificationChannel channel,
        NotificationPriority priority,
        String title,
        String body,
        boolean read,
        Instant createdAt,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt,
        Map<String, String> metadata
) {
    public static NotificationResponseDTO fromEntity(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getRecipientId(),
                notification.getRecipientEmail(),
                notification.getType(),
                notification.getChannel(),
                notification.getPriority(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getDeliveredAt(),
                notification.getReadAt(),
                notification.getMetadata()
        );
    }
}
