package com.mobflow.notificationservice.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.notificationservice.kafka.events.CommentNotificationEvent;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationFactory;
import com.mobflow.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CommentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationFactory notificationFactory;
    private final NotificationService notificationService;

    public CommentEventConsumer(
            ObjectMapper objectMapper,
            NotificationFactory notificationFactory,
            NotificationService notificationService
    ) {
        this.objectMapper = objectMapper;
        this.notificationFactory = notificationFactory;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.social-comment}", groupId = "notification-service")
    public void consume(String payload) {
        try {
            CommentNotificationEvent event = objectMapper.readValue(payload, CommentNotificationEvent.class);
            Notification notification = notificationFactory.createCommentNotification(event);
            if (notification != null) {
                notificationService.save(notification);
            } else {
                log.warn("No notification created for comment event type: {}", event.eventType());
            }
        } catch (Exception exception) {
            log.warn("Failed to process comment event payload: {}", payload, exception);
        }
    }
}
