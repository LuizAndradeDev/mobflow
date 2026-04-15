package com.mobflow.notificationservice.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.notificationservice.kafka.events.AuthNotificationEvent;
import com.mobflow.notificationservice.mail.MailService;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationType;
import com.mobflow.notificationservice.service.NotificationFactory;
import com.mobflow.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuthEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuthEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationFactory notificationFactory;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final String authTopic;

    public AuthEventConsumer(
            ObjectMapper objectMapper,
            NotificationFactory notificationFactory,
            NotificationService notificationService,
            MailService mailService,
            @Value("${app.kafka.topics.auth}") String authTopic
    ) {
        this.objectMapper = objectMapper;
        this.notificationFactory = notificationFactory;
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.authTopic = authTopic;
    }

    @KafkaListener(topics = "${app.kafka.topics.auth}", groupId = "notification-service")
    public void consume(String payload) {
        try {
            AuthNotificationEvent event = objectMapper.readValue(payload, AuthNotificationEvent.class);
            if (!"EMAIL_CONFIRMATION".equals(event.eventType())) {
                log.warn("Ignoring unsupported auth event type: {}", event.eventType());
                return;
            }

            Notification notification = notificationFactory.createAuthNotification(event);
            Notification persisted = notificationService.save(notification);
            if (persisted.getType() == NotificationType.EMAIL_CONFIRMATION) {
                mailService.sendConfirmationEmail(persisted, event.confirmationUrl());
            }
        } catch (Exception exception) {
            log.warn("Failed to process auth event payload on topic {}: {}", authTopic, payload, exception);
        }
    }
}
