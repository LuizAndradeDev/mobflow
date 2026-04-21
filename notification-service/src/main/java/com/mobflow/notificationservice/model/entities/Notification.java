package com.mobflow.notificationservice.model.entities;

import  com.mobflow.notificationservice.model.enums.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private String recipientId;
    private String recipientEmail;

    private String title;
    private String body;

    @Indexed
    private NotificationType type;

    @Indexed
    private NotificationChannel channel;

    private NotificationPriority priority;

    private int retryCount;
    private int maxRetries;

    private Map<String, String> metadata;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private boolean read = false;
}
