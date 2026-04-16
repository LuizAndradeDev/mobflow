package com.mobflow.socialservice.testsupport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class JwtTestHelper {

    private JwtTestHelper() {
    }

    public static String token(String secretKey, UUID authId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("authId", authId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(signingKey(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public static UsernamePasswordAuthenticationToken authentication(UUID authId, String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                authId,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private static Key signingKey(String secretKey) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
