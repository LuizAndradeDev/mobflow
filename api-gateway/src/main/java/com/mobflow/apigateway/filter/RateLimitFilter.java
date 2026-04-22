package com.mobflow.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    private final ConcurrentMap<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiPath = exchange.getRequest().getPath().value();

        if (!apiPath.startsWith("/api/") || apiPath.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        String clientId = resolveClientId(exchange.getRequest());

        if (isRateLimited(clientId)) {
            log.warn("RATE_LIMIT_EXCEEDED | clientId={} | path={}", clientId, apiPath);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private String resolveClientId(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        String ip = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    private boolean isRateLimited(String clientId) {
        Instant now = Instant.now();
        RateLimitEntry entry = rateLimitMap.compute(clientId, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new RateLimitEntry(now, 1);
            }
            existing.count++;
            return existing;
        });
        return entry.count > requestsPerMinute;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private static class RateLimitEntry {
        final Instant windowStart;
        int count;

        RateLimitEntry(Instant windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        boolean isExpired(Instant now) {
            return now.minusSeconds(60).isAfter(windowStart);
        }
    }
}