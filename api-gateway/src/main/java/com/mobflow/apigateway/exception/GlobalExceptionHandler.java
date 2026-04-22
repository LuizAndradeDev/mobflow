package com.mobflow.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String error = "Internal Server Error";
        String message = "An unexpected error occurred";

        if (ex instanceof BadJwtException || ex instanceof JwtException) {
            status = HttpStatus.UNAUTHORIZED.value();
            error = "Unauthorized";
            message = "Invalid or missing authentication token";
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            status = responseStatusException.getStatusCode().value();
            error = HttpStatus.valueOf(status).getReasonPhrase();
            message = responseStatusException.getReason();
        }

        log.error("ERROR | path={} | status={} | error={} | message={}",
                path, status, error, ex.getMessage());

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", path
        );

        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(
                        ("{" +
                                "\"timestamp\":\"" + body.get("timestamp") + "\"," +
                                "\"status\":" + body.get("status") + "," +
                                "\"error\":\"" + body.get("error") + "\"," +
                                "\"message\":\"" + body.get("message") + "\"," +
                                "\"path\":\"" + body.get("path") + "\"" +
                                "}").getBytes()
                ))
        );
    }
}