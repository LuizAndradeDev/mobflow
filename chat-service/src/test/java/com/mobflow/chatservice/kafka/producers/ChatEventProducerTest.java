package com.mobflow.chatservice.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.chatservice.kafka.events.NewChatMessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class ChatEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ChatEventProducer chatEventProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatEventProducer = new ChatEventProducer(kafkaTemplate, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(chatEventProducer, "socialEventsTopic", "social.events");
    }

    @Test
    void publishNewChatMessage_validEvent_sendsSerializedPayload() {
        NewChatMessageEvent event = NewChatMessageEvent.builder()
                .eventType("NEW_CHAT_MESSAGE")
                .messageId("message-1")
                .conversationId("conversation-1")
                .senderId("sender-1")
                .recipientId("recipient-1")
                .contentPreview("hello")
                .createdAt(Instant.now())
                .build();

        chatEventProducer.publishNewChatMessage(event);

        verify(kafkaTemplate).send(eq("social.events"), eq("message-1"), startsWith("{"));
    }

    @Test
    void publishNewChatMessage_sendFailure_doesNotPropagateException() {
        NewChatMessageEvent event = NewChatMessageEvent.builder()
                .eventType("NEW_CHAT_MESSAGE")
                .messageId("message-1")
                .build();
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(eq("social.events"), eq("message-1"), startsWith("{"));

        chatEventProducer.publishNewChatMessage(event);

        verify(kafkaTemplate).send(eq("social.events"), eq("message-1"), startsWith("{"));
    }
}
