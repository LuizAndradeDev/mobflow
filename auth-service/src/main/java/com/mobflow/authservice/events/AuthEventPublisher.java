package com.mobflow.authservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.authservice.model.entities.UserCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuthEventPublisher {

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
            kafkaTemplate.send(topicName, userCredential.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }

    private String buildConfirmationUrl(String token) {
        return appBaseUrl + "/confirm-email?token=" + token;
    }
}
