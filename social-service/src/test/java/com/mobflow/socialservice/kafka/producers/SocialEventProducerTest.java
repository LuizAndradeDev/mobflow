package com.mobflow.socialservice.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.kafka.events.CommentNotificationEvent;
import com.mobflow.socialservice.kafka.events.FriendRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private SocialEventProducer socialEventProducer;

    @BeforeEach
    void setUp() {
        socialEventProducer = new SocialEventProducer(
                kafkaTemplate,
                Jackson2ObjectMapperBuilder.json().build(),
                "social-comment-events",
                "social-friendship-events"
        );
    }

    @Test
    void publishCommentEvent_validEvent_serializesAndPublishesPayload() {
        CommentNotificationEvent event = new CommentNotificationEvent(
                "COMMENT_CREATED",
                "recipient-1",
                "actor-1",
                "john_dev",
                "task-1",
                "workspace-1",
                "comment-1",
                "Prepare roadmap",
                "Hello team",
                null,
                Instant.now()
        );

        socialEventProducer.publishCommentEvent(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("social-comment-events"), org.mockito.ArgumentMatchers.eq("comment-1"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"eventType\":\"COMMENT_CREATED\"");
        assertThat(payloadCaptor.getValue()).contains("\"commentId\":\"comment-1\"");
    }

    @Test
    void publishFriendRequestEvent_validEvent_serializesAndPublishesPayload() {
        FriendRequestEvent event = new FriendRequestEvent(
                "FRIEND_REQUEST_SENT",
                "recipient-1",
                "actor-1",
                "john_dev",
                "request-1",
                "subject-1",
                "mary_dev",
                Instant.now()
        );

        socialEventProducer.publishFriendRequestEvent(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("social-friendship-events"), org.mockito.ArgumentMatchers.eq("request-1"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"eventType\":\"FRIEND_REQUEST_SENT\"");
        assertThat(payloadCaptor.getValue()).contains("\"requestId\":\"request-1\"");
    }

    @Test
    void publishCommentEvent_kafkaFailure_doesNotThrowException() {
        CommentNotificationEvent event = new CommentNotificationEvent(
                "COMMENT_CREATED",
                "recipient-1",
                "actor-1",
                "john_dev",
                "task-1",
                "workspace-1",
                "comment-1",
                "Prepare roadmap",
                "Hello team",
                null,
                Instant.now()
        );
        doThrow(new RuntimeException("kafka down"))
                .when(kafkaTemplate)
                .send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        assertThatCode(() -> socialEventProducer.publishCommentEvent(event))
                .doesNotThrowAnyException();
    }
}
