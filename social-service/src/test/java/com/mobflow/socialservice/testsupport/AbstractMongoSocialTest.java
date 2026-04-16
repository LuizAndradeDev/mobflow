package com.mobflow.socialservice.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

public abstract class AbstractMongoSocialTest {

    @SuppressWarnings("resource")
    private static final MongoDBContainer MONGO_DB_CONTAINER =
            new MongoDBContainer("mongo:7.0");

    static {
        MONGO_DB_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
    }
}
