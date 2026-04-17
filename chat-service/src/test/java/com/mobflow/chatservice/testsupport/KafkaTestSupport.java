package com.mobflow.chatservice.testsupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

public final class KafkaTestSupport {

    private KafkaTestSupport() {
    }

    public static Consumer<String, String> createConsumer(EmbeddedKafkaBroker embeddedKafkaBroker, String groupId) {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        );
        return consumerFactory.createConsumer();
    }
}
