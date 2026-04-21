package com.mobflow.socialservice.client;

import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.resilience.InternalCallPolicy;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getTaskContext_callsTaskServiceUsingTasksContextPath() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID createdByAuthId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> secretHeader = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks/internal/tasks/" + taskId + "/context", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            secretHeader.set(exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
            writeJson(exchange, 200, """
                    {
                      "taskId": "%s",
                      "workspaceId": "%s",
                      "createdByAuthId": "%s",
                      "assigneeAuthId": "%s",
                      "taskTitle": "Task title"
                    }
                    """.formatted(taskId, workspaceId, createdByAuthId, assigneeAuthId));
        });
        server.start();

        TaskServiceClient client = new TaskServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret"
        );

        TaskServiceClient.TaskCommentContextResponse response = client.getTaskContext(taskId);

        assertThat(requestPath.get()).isEqualTo("/tasks/internal/tasks/" + taskId + "/context");
        assertThat(secretHeader.get()).isEqualTo("internal-secret");
        assertThat(response.taskId()).isEqualTo(taskId);
        assertThat(response.workspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void getTaskContext_whenTaskServiceReturnsNotFound_throwsTaskNotFound() throws Exception {
        UUID taskId = UUID.randomUUID();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks/internal/tasks/" + taskId + "/context", exchange ->
                writeJson(exchange, 404, "{\"title\":\"TASK_NOT_FOUND\"}"));
        server.start();

        TaskServiceClient client = new TaskServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret"
        );

        assertThatThrownBy(() -> client.getTaskContext(taskId))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("Task not found");
    }

    @Test
    void getTaskContext_retriesTransientServerError() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID createdByAuthId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks/internal/tasks/" + taskId + "/context", exchange -> {
            if (attempts.incrementAndGet() == 1) {
                writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}");
                return;
            }
            writeJson(exchange, 200, """
                    {
                      "taskId": "%s",
                      "workspaceId": "%s",
                      "createdByAuthId": "%s",
                      "assigneeAuthId": "%s",
                      "taskTitle": "Task title"
                    }
                    """.formatted(taskId, workspaceId, createdByAuthId, assigneeAuthId));
        });
        server.start();

        TaskServiceClient client = new TaskServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret",
                InternalCallPolicy.critical(
                        "social-task-context-retry-test",
                        2,
                        Duration.ofMillis(1),
                        1.0,
                        4,
                        2,
                        50.0f,
                        Duration.ofSeconds(30)
                )
        );

        TaskServiceClient.TaskCommentContextResponse response = client.getTaskContext(taskId);

        assertThat(attempts).hasValue(2);
        assertThat(response.workspaceId()).isEqualTo(workspaceId);
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
