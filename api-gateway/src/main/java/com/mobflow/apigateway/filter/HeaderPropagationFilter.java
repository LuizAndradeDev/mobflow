package com.mobflow.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class HeaderPropagationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> mutateRequest(exchange, authentication))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private ServerWebExchange mutateRequest(ServerWebExchange exchange, JwtAuthenticationToken authentication) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        String userId = authentication.getToken().getClaimAsString("authId");
        if (userId == null || userId.isBlank()) {
            userId = authentication.getToken().getSubject();
        }

        String roles = resolveRoles(authentication);

        if (userId != null && !userId.isBlank()) {
            requestBuilder.header(USER_ID_HEADER, userId);
        }
        if (roles != null && !roles.isBlank()) {
            requestBuilder.header(USER_ROLES_HEADER, roles);
        }

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    private String resolveRoles(JwtAuthenticationToken authentication) {
        Object rolesClaim = authentication.getToken().getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        return rolesClaim != null ? String.valueOf(rolesClaim) : null;
    }
}
