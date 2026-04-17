package com.mobflow.chatservice.client;

import com.mobflow.chatservice.exception.ChatServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
