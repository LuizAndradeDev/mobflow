package com.mobflow.apigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private static DisposableServer mockService;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startMockService() {
        mockService = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes -> routes.route(request -> true, (request, response) -> {
                    Map<String, String> payload = new LinkedHashMap<>();
                    payload.put("path", request.uri());
                    addIfPresent(payload, "authorization", request.requestHeaders().get("Authorization"));
                    addIfPresent(payload, "xUserId", request.requestHeaders().get("X-User-Id"));
                    addIfPresent(payload, "xUserRoles", request.requestHeaders().get("X-User-Roles"));
                    addIfPresent(payload, "xCorrelationId", request.requestHeaders().get("X-Correlation-Id"));

                    try {
                        return response.header("Content-Type", "application/json")
                                .sendString(reactor.core.publisher.Mono.just(OBJECT_MAPPER.writeValueAsString(payload)))
                                .then();
                    } catch (Exception exception) {
                        return response.status(500).send();
                    }
                }))
                .bindNow();
    }

    @AfterAll
    static void stopMockService() {
        if (mockService != null) {
            mockService.disposeNow();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("gateway.jwt.secret-key", () -> JWT_SECRET);

        registry.add("gateway.routes.auth-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.user-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.workspace-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.task-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.notification-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.social-service-uri", GatewayIntegrationTest::mockBaseUrl);
        registry.add("gateway.routes.chat-service-uri", GatewayIntegrationTest::mockBaseUrl);
    }

    @Test
    void shouldExposePublicAuthRouteWithoutJwt() {
        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(Map.of("username", "demo", "password", "secret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/api/auth/login")
                .jsonPath("$.authorization").doesNotExist();
    }

    @Test
    void shouldRejectProtectedRoutesWithoutJwt() {
        webTestClient.get()
                .uri("/api/workspaces")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRewriteTaskRoutesAndPropagateIdentityHeaders() {
        webTestClient.get()
                .uri("/api/tasks/workspaces/workspace-123/boards")
                .header("Authorization", "Bearer " + jwt("user-subject", "auth-123", "ROLE_USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/tasks/api/workspaces/workspace-123/boards")
                .jsonPath("$.xUserId").isEqualTo("auth-123")
                .jsonPath("$.xUserRoles").isEqualTo("ROLE_USER")
                .jsonPath("$.xCorrelationId").value(value ->
                        org.junit.jupiter.api.Assertions.assertFalse(String.valueOf(value).isBlank()));
    }

    @Test
    void shouldMatchNotificationBasePathWithoutTrailingWildcardSegment() {
        webTestClient.get()
                .uri("/api/notifications")
                .header("Authorization", "Bearer " + jwt("user-subject", "auth-123", "ROLE_USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/api/notifications");
    }

    @Test
    void shouldRewriteSocialRoutesToServiceContextPath() {
        webTestClient.get()
                .uri("/api/social/friends")
                .header("Authorization", "Bearer " + jwt("user-subject", "auth-123", "ROLE_USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/social/api/friends");
    }

    private static String mockBaseUrl() {
        return "http://127.0.0.1:" + mockService.port();
    }

    private static void addIfPresent(Map<String, String> payload, String key, String value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static String jwt(String subject, String authId, String roles) {
        SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(subject)
                .claim("authId", authId)
                .claim("roles", roles)
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(now.plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
