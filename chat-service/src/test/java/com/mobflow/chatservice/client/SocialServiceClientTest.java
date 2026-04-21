package com.mobflow.chatservice.client;

import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.resilience.InternalCallPolicy;
import com.mobflow.chatservice.resilience.InternalHttpClientSupport;
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

class SocialServiceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void validateFriendshipRequired_callsSocialServiceUsingSocialContextPath() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> secretHeader = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId, exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            secretHeader.set(exchange.getRequestHeaders().getFirst("X-Internal-Secret"));
            writeJson(exchange, 200, "{}");
        });
        server.start();

        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret"
        );

        client.validateFriendshipRequired(authId, targetAuthId);

        assertThat(requestPath.get())
                .isEqualTo("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId);
        assertThat(secretHeader.get()).isEqualTo("internal-secret");
    }

    @Test
    void validateFriendshipRequired_whenSocialServiceReturnsNotFound_throwsFriendshipRequired() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId, exchange ->
                writeJson(exchange, 404, "{\"title\":\"FRIENDSHIP_NOT_FOUND\"}"));
        server.start();

        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret"
        );

        assertThatThrownBy(() -> client.validateFriendshipRequired(authId, targetAuthId))
                .isInstanceOf(ChatServiceException.class)
                .hasMessage("Only friends can create conversations or exchange messages");
    }

    @Test
    void validateFriendshipRequired_whenSocialServiceIsUnavailable_throwsServiceUnavailable() {
        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder().baseUrl("http://localhost:65535").build(),
                "internal-secret"
        );

        assertThatThrownBy(() -> client.validateFriendshipRequired(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ChatServiceException.class)
                .hasMessage("Unable to validate friendship with social-service");
    }

    @Test
    void validateFriendshipRequired_retriesTransientServerError() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId, exchange -> {
            if (attempts.incrementAndGet() == 1) {
                writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}");
                return;
            }
            writeJson(exchange, 200, "{}");
        });
        server.start();

        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret",
                fastCriticalPolicy("chat-social-retry-test", 2)
        );

        client.validateFriendshipRequired(authId, targetAuthId);

        assertThat(attempts).hasValue(2);
    }

    @Test
    void validateFriendshipRequired_opensCircuitAfterRepeatedTransientFailures() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId, exchange -> {
            attempts.incrementAndGet();
            writeJson(exchange, 503, "{\"title\":\"temporarily unavailable\"}");
        });
        server.start();

        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort()).build(),
                "internal-secret",
                InternalCallPolicy.critical(
                        "chat-social-circuit-test",
                        1,
                        Duration.ofMillis(1),
                        1.0,
                        2,
                        2,
                        50.0f,
                        Duration.ofSeconds(30)
                )
        );

        assertThatThrownBy(() -> client.validateFriendshipRequired(authId, targetAuthId))
                .isInstanceOf(ChatServiceException.class);
        assertThatThrownBy(() -> client.validateFriendshipRequired(authId, targetAuthId))
                .isInstanceOf(ChatServiceException.class);
        assertThatThrownBy(() -> client.validateFriendshipRequired(authId, targetAuthId))
                .isInstanceOf(ChatServiceException.class)
                .hasMessage("Unable to validate friendship with social-service");

        assertThat(attempts).hasValue(2);
    }

    @Test
    void validateFriendshipRequired_whenReadTimesOut_throwsServiceUnavailable() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/social/internal/social/friendships/" + authId + "/friends/" + targetAuthId, exchange -> {
            try {
                Thread.sleep(400);
                writeJson(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            }
        });
        server.start();

        SocialServiceClient client = new SocialServiceClient(
                RestClient.builder()
                        .baseUrl("http://localhost:" + server.getAddress().getPort())
                        .requestFactory(InternalHttpClientSupport.requestFactory(Duration.ofMillis(100), Duration.ofMillis(100)))
                        .build(),
                "internal-secret",
                InternalCallPolicy.noOp()
        );

        assertThatThrownBy(() -> client.validateFriendshipRequired(authId, targetAuthId))
                .isInstanceOf(ChatServiceException.class)
                .hasMessage("Unable to validate friendship with social-service");
    }

    private static InternalCallPolicy fastCriticalPolicy(String name, int maxAttempts) {
        return InternalCallPolicy.critical(
                name,
                maxAttempts,
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
