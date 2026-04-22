package com.mobflow.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.nanoTime();

        String correlationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        }

        final String corrId = correlationId;
        final String method = request.getMethod().name();
        final String path = request.getPath().value();

        log.info("REQUEST | method={} | path={} | correlationId={}",
                method, path, corrId);

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = TimeUnit.MILLISECONDS.convert(
                            System.nanoTime() - startTime,
                            TimeUnit.NANOSECONDS);

                    int statusCode = exchange.getResponse().getStatusCode().value();

                    log.info("RESPONSE | method={} | path={} | status={} | duration={}ms | correlationId={}",
                            method, path, statusCode, duration, corrId);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}