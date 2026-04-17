package com.mobflow.chatservice.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.chatservice.kafka.events.NewChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ChatEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.social-events}")
    private String socialEventsTopic;

    public void publishNewChatMessage(NewChatMessageEvent event) {
        try {
            kafkaTemplate.send(socialEventsTopic, event.getMessageId(), objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.warn("Failed to publish chat event for message {}", event.getMessageId(), exception);
        }
    }
}
