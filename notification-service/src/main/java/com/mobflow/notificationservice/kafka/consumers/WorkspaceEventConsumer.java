package com.mobflow.notificationservice.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.notificationservice.kafka.events.WorkspaceNotificationEvent;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationFactory;
import com.mobflow.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceEventConsumer.class);

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
            } else {
                log.warn("No notification created for workspace event type: {}", event.eventType());
            }
        } catch (Exception exception) {
            log.warn("Failed to process workspace event payload: {}", payload, exception);
        }
    }
}
