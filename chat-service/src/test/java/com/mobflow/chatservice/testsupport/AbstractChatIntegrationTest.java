package com.mobflow.chatservice.testsupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "security.jwt.secret-key=Y2hhdC1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS1jaGF0LXNlcnZpY2UtdGVzdA==",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.data.mongodb.auto-index-creation=true",
        "social.service.url=http://localhost:8085",
        "internal.secret=test-internal-secret",
        "app.kafka.topics.social-events=social.events"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"social.events"})
public abstract class AbstractChatIntegrationTest extends AbstractMongoChatTest {

    protected static final String TEST_SECRET =
            "Y2hhdC1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS1jaGF0LXNlcnZpY2UtdGVzdA==";

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

    protected MockHttpServletRequestBuilder withChatContextPath(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/chat");
    }
}
