package com.mobflow.taskservice.client;

import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.resilience.InternalCallPolicy;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getMemberRole_retriesTransientWorkspaceFailure() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/workspaces/" + workspaceId + "/members/" + authId + "/role", exchange -> {
            if (attempts.incrementAndGet() == 1) {
                writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}");
                return;
            }
            writeJson(exchange, 200, "{\"role\":\"ADMIN\"}");
        });
        server.start();

        WorkspaceClient client = new WorkspaceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret",
                fastCriticalPolicy("task-workspace-retry-test")
        );

        WorkspaceClient.MemberRoleResponse response = client.getMemberRole(workspaceId, authId);

        assertThat(attempts).hasValue(2);
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void getMemberRole_whenWorkspaceServiceIsUnavailable_throwsServiceUnavailable() {
        WorkspaceClient client = new WorkspaceClient(
                RestClient.builder().baseUrl("http://localhost:65535").build(),
                "internal-secret",
                fastCriticalPolicy("task-workspace-unavailable-test")
        );

        assertThatThrownBy(() -> client.getMemberRole(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(TaskServiceException.class)
                .hasMessage("WORKSPACE_SERVICE_UNAVAILABLE");
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
