package com.mobflow.socialservice.testsupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

@SpringBootTest(properties = {
        "security.jwt.secret-key=c29jaWFsLXNlcnZpY2UtdGVzdC1zZWNyZXQta2V5LXNvY2lhbC1zZXJ2aWNlLXRlc3Q=",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.data.mongodb.auto-index-creation=true",
        "auth.service.url=http://localhost:8080",
        "task.service.url=http://localhost:8083",
        "user.service.url=http://localhost:8081",
        "workspace.service.url=http://localhost:8082",
        "internal.secret=test-internal-secret",
        "app.kafka.topics.social-comment=social-comment-events",
        "app.kafka.topics.social-friendship=social-friendship-events"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"social-comment-events", "social-friendship-events"})
public abstract class AbstractSocialIntegrationTest extends AbstractMongoSocialTest {

    protected static final String TEST_SECRET =
            "c29jaWFsLXNlcnZpY2UtdGVzdC1zZWNyZXQta2V5LXNvY2lhbC1zZXJ2aWNlLXRlc3Q=";

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafkaBroker;

    protected String bearerToken(UUID authId, String username) {
        return "Bearer " + JwtTestHelper.token(TEST_SECRET, authId, username);
    }

    protected Consumer<String, String> consumer(String topic, String groupId) {
        Consumer<String, String> consumer = KafkaTestSupport.createConsumer(embeddedKafkaBroker, groupId);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    protected MockHttpServletRequestBuilder withSocialContextPath(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/social");
    }
}
