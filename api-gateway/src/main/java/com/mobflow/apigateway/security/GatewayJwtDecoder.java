package com.mobflow.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Base64;
import java.util.Map;

public class GatewayJwtDecoder implements ReactiveJwtDecoder {

    private final Key signingKey;

    public GatewayJwtDecoder(String jwtSecret) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return Mono.fromCallable(() -> {
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                return new Jwt(token,
                        claims.getIssuedAt().toInstant(),
                        claims.getExpiration().toInstant(),
                        Map.of("alg", "HS256", "typ", "JWT"),
                        claims
                );
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                throw new BadJwtException("Token expired", e);
            } catch (io.jsonwebtoken.JwtException e) {
                throw new BadJwtException("Invalid token: " + e.getMessage(), e);
            }
        });
    }
}