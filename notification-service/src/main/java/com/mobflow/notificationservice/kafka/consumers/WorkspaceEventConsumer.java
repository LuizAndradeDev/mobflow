package com.mobflow.notificationservice.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.notificationservice.kafka.events.WorkspaceNotificationEvent;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationFactory;
import com.mobflow.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationFactory notificationFactory;
    private final NotificationService notificationService;

    public WorkspaceEventConsumer(
            ObjectMapper objectMapper,
            NotificationFactory notificationFactory,
            NotificationService notificationService
    ) {
        this.objectMapper = objectMapper;
        this.notificationFactory = notificationFactory;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.workspace}", groupId = "notification-service")
    public void consume(String payload) {
        try {
            WorkspaceNotificationEvent event = objectMapper.readValue(payload, WorkspaceNotificationEvent.class);
            Notification notification = notificationFactory.createWorkspaceNotification(event);
            if (notification != null) {
                notificationService.save(notification);
            }
        } catch (Exception ignored) {
        }
    }
}
