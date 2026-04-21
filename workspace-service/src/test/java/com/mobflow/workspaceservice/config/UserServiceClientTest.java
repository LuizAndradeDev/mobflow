package com.mobflow.workspaceservice.config;

import com.mobflow.workspaceservice.exception.UserServiceUnavailableException;
import com.mobflow.workspaceservice.resilience.InternalCallPolicy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void resolveAuthIdByUsername_retriesTransientUserServiceFailure() throws Exception {
        UUID authId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username/mary", exchange -> {
            if (attempts.incrementAndGet() == 1) {
                writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}");
                return;
            }
            writeJson(exchange, 200, """
                    {
                      "authId": "%s",
                      "displayName": "Mary",
                      "avatarUrl": null
                    }
                    """.formatted(authId));
        });
        server.start();

        UserServiceClient client = clientForServer("workspace-user-retry-test", InternalCallPolicy.noOp());

        UUID resolvedAuthId = client.resolveAuthIdByUsername("mary");

        assertThat(attempts).hasValue(2);
        assertThat(resolvedAuthId).isEqualTo(authId);
    }

    @Test
    void resolveAuthIdByUsername_whenUserServiceIsUnavailable_throwsServiceUnavailable() {
        UserServiceClient client = new UserServiceClient(
                RestClient.builder().baseUrl("http://localhost:65535").build(),
                "internal-secret",
                fastCriticalPolicy("workspace-user-unavailable-test"),
                InternalCallPolicy.noOp()
        );

        assertThatThrownBy(() -> client.resolveAuthIdByUsername("mary"))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessage("USER_SERVICE_UNAVAILABLE");
    }

    @Test
    void fetchProfilesBatch_whenUserServiceFails_returnsEmptyMap() throws Exception {
        UUID authId = UUID.randomUUID();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/batch", exchange ->
                writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}"));
        server.start();

        UserServiceClient client = clientForServer("workspace-user-lookup-unused", InternalCallPolicy.noOp());

        Map<UUID, UserServiceClient.UserProfileResponse> profiles = client.fetchProfilesBatch(List.of(authId));

        assertThat(profiles).isEmpty();
    }

    private UserServiceClient clientForServer(String lookupPolicyName, InternalCallPolicy profilePolicy) {
        return new UserServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret",
                fastCriticalPolicy(lookupPolicyName),
                profilePolicy
        );
    }

    private static InternalCallPolicy fastCriticalPolicy(String name) {
        return InternalCallPolicy.critical(
                name,
                2,
                Duration.ofMillis(1),
                1.0,
                4,
                2,
                50.0f,
                Duration.ofSeconds(30)
        );
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
