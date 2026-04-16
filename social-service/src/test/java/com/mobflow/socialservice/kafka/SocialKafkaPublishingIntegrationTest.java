package com.mobflow.socialservice.kafka;

import com.mobflow.socialservice.kafka.events.CommentNotificationEvent;
import com.mobflow.socialservice.kafka.events.FriendRequestEvent;
import com.mobflow.socialservice.kafka.producers.SocialEventProducer;
import com.mobflow.socialservice.testsupport.AbstractSocialIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class SocialKafkaPublishingIntegrationTest extends AbstractSocialIntegrationTest {

    @Autowired
    private SocialEventProducer socialEventProducer;

    private Consumer<String, String> commentConsumer;
    private Consumer<String, String> friendshipConsumer;

    @BeforeEach
    void setUp() {
        commentConsumer = consumer("social-comment-events", "social-kafka-comment-" + UUID.randomUUID());
        friendshipConsumer = consumer("social-friendship-events", "social-kafka-friend-" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        commentConsumer.close();
        friendshipConsumer.close();
    }

    @Test
    void publishCommentEvent_validPayload_writesExpectedStructureToKafka() {
        socialEventProducer.publishCommentEvent(new CommentNotificationEvent(
                "USER_MENTIONED",
                "recipient-1",
                "actor-1",
                "john_dev",
                "task-1",
                "workspace-1",
                "comment-1",
                "Prepare roadmap",
                "Hello @mary_dev",
                "mary_dev",
                Instant.now()
        ));

        List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    KafkaTestUtils.getRecords(commentConsumer, Duration.ofMillis(250))
                            .forEach(records::add);
                    assertThat(records).isNotEmpty();
                });

        assertThat(records.getFirst().value())
                .contains("\"eventType\":\"USER_MENTIONED\"")
                .contains("\"mentionedUsername\":\"mary_dev\"");
    }

    @Test
    void publishFriendRequestEvent_validPayload_writesExpectedStructureToKafka() {
        socialEventProducer.publishFriendRequestEvent(new FriendRequestEvent(
                "FRIEND_REQUEST_SENT",
                "recipient-1",
                "actor-1",
                "john_dev",
                "request-1",
                "subject-1",
                "mary_dev",
                Instant.now()
        ));

        List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    KafkaTestUtils.getRecords(friendshipConsumer, Duration.ofMillis(250))
                            .forEach(records::add);
                    assertThat(records).isNotEmpty();
                });

        assertThat(records.getFirst().value())
                .contains("\"eventType\":\"FRIEND_REQUEST_SENT\"")
                .contains("\"requestId\":\"request-1\"");
    }
}
