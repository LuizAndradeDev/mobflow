package com.mobflow.authservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.authservice.model.entities.UserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class AuthEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final String appBaseUrl;

    public AuthEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.auth}") String topicName,
            @Value("${app.base-url}") String appBaseUrl
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
        this.appBaseUrl = appBaseUrl;
    }

    public void publishEmailConfirmation(UserCredential userCredential) {
        try {
            AuthNotificationEvent event = new AuthNotificationEvent(
                    "EMAIL_CONFIRMATION",
                    userCredential.getId().toString(),
                    userCredential.getEmail(),
                    userCredential.getUsername(),
                    userCredential.getConfirmationToken(),
                    buildConfirmationUrl(userCredential.getConfirmationToken()),
                    Instant.now()
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topicName, userCredential.getId().toString(), payload).get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            log.warn("Failed to publish email confirmation event for user {}", userCredential.getId(), exception);
        }
    }

    private String buildConfirmationUrl(String token) {
        return appBaseUrl.replaceAll("/+$", "") + "/confirm-email?token=" + token;
    }
}
