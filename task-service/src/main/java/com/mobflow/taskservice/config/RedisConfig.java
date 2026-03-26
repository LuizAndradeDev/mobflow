package com.mobflow.taskservice.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    // Redis connection is auto-configured by Spring Boot.
    // Caching is intentionally disabled for task-service at this stage.
    // The complexity of serializing polymorphic List<T> with DefaultTyping
    // is not worth the performance gain for this service's current scale.
}