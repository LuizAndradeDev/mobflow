package com.mobflow.workspaceservice.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresWorkspaceServiceTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("workspace_service_test")
                    .withUsername("mobflow")
                    .withPassword("mobflow");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.flyway.password", POSTGRESQL_CONTAINER::getPassword);
    }
}
