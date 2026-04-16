package com.mobflow.socialservice.kafka.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.kafka.events.CommentNotificationEvent;
import com.mobflow.socialservice.kafka.events.FriendRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SocialEventProducer {

    private static final Logger log = LoggerFactory.getLogger(SocialEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String commentTopic;
    private final String friendshipTopic;

    public SocialEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.social-comment}") String commentTopic,
            @Value("${app.kafka.topics.social-friendship}") String friendshipTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.commentTopic = commentTopic;
        this.friendshipTopic = friendshipTopic;
    }

    public void publishCommentEvent(CommentNotificationEvent event) {
        try {
            kafkaTemplate.send(commentTopic, event.commentId(), objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.warn("Failed to publish comment event for comment {}", event.commentId(), exception);
        }
    }

    public void publishFriendRequestEvent(FriendRequestEvent event) {
        try {
            kafkaTemplate.send(friendshipTopic, event.requestId(), objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.warn("Failed to publish friendship event for request {}", event.requestId(), exception);
        }
    }
}
