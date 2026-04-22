package com.mobflow.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
public class HeaderPropagationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst("Authorization");

        String userId = null;
        String roles = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String payload = token.split("\\.")[1];
                String decoded = new String(Base64.getDecoder().decode(payload + "==".substring(0, (4 - payload.length() % 4) % 4)));
                int subjectStart = decoded.indexOf("\"sub\":");
                if (subjectStart > 0) {
                    int subjectEnd = decoded.indexOf(",", subjectStart);
                    if (subjectEnd < 0) subjectEnd = decoded.indexOf("}", subjectStart);
                    if (subjectEnd > subjectStart) {
                        String sub = decoded.substring(subjectStart + 6, subjectEnd - 1);
                        if (sub.startsWith("\"")) sub = sub.substring(1);
                        if (sub.endsWith("\"")) sub = sub.substring(0, sub.length() - 1);
                        userId = sub;
                    }
                }
                int authIdStart = decoded.indexOf("\"authId\":");
                if (authIdStart > 0) {
                    int authIdEnd = decoded.indexOf(",", authIdStart);
                    if (authIdEnd < 0) authIdEnd = decoded.indexOf("}", authIdStart);
                    if (authIdEnd > authIdStart) {
                        String authId = decoded.substring(authIdStart + 8, authIdEnd - 1);
                        if (authId.startsWith("\"")) authId = authId.substring(1);
                        if (authId.endsWith("\"")) authId = authId.substring(0, authId.length() - 1);
                        userId = authId;
                    }
                }
                int rolesStart = decoded.indexOf("\"roles\":");
                if (rolesStart > 0) {
                    int rolesEnd = decoded.indexOf(",", rolesStart);
                    if (rolesEnd < 0) rolesEnd = decoded.indexOf("}", rolesStart);
                    if (rolesEnd > rolesStart) {
                        roles = decoded.substring(rolesStart + 8, rolesEnd - 1);
                        if (roles.startsWith("\"")) roles = roles.substring(1);
                        if (roles.endsWith("\"")) roles = roles.substring(0, roles.length() - 1);
                    }
                }
            } catch (Exception e) {
            }
        }

        ServerHttpRequest.Builder requestBuilder = request.mutate();

        if (userId != null && !userId.isBlank()) {
            requestBuilder.header(USER_ID_HEADER, userId);
        }
        if (roles != null && !roles.isBlank()) {
            requestBuilder.header(USER_ROLES_HEADER, roles);
        }

        ServerHttpRequest modifiedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}