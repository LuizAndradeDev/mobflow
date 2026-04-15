package com.mobflow.authservice.services;

import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JWTServiceTest {

    private static final String SECRET_KEY =
            "c3VwZXItc2VjdXJlLXRlc3Qta2V5LWZvci1tb2JmbG93LWF1dGgtc2VydmljZS0xMjM0NTY3ODkw";

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Test
    void generateTokenShouldExposeUsernameAsSubject() {
        // Given
        UserCredential user = UserCredential.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void generateTokenShouldIncludeAuthIdClaimForUserCredential() {
        // Given
        UUID authId = UUID.randomUUID();
        UserCredential user = UserCredential.builder()
                .id(authId)
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        // When
        String token = jwtService.generateToken(user);

        // Then
        String extractedAuthId = jwtService.extractClaim(token, claims -> claims.get("authId", String.class));
        assertThat(extractedAuthId).isEqualTo(authId.toString());
    }

    @Test
    void isTokenValidShouldReturnTrueForMatchingUser() {
        // Given
        UserCredential user = UserCredential.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();
        String token = jwtService.generateToken(user);

        // When
        boolean valid = jwtService.isTokenValid(token, user);

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValidShouldReturnFalseForDifferentUser() {
        // Given
        UserCredential user = UserCredential.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();
        String token = jwtService.generateToken(user);
        User differentUser = new User("mary", "encoded-password", List.of());

        // When
        boolean valid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void invalidTokenShouldReturnNullClaimsAndBeInvalid() {
        // Given
        String invalidToken = "invalid.jwt.token";
        User user = new User("john", "encoded-password", List.of());

        // When
        String username = jwtService.extractUsername(invalidToken);
        Claims claims = jwtService.extractClaim(invalidToken, existingClaims -> existingClaims);
        boolean valid = jwtService.isTokenValid(invalidToken, user);

        // Then
        assertThat(username).isNull();
        assertThat(claims).isNull();
        assertThat(valid).isFalse();
    }

    @Test
    void expiredTokenShouldStillExposeClaimsButBeInvalid() throws InterruptedException {
        // Given
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        UserCredential user = UserCredential.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        String token = jwtService.generateToken(user);
        Thread.sleep(50L);

        // When
        String username = jwtService.extractUsername(token);
        String extractedAuthId = jwtService.extractClaim(token, claims -> claims.get("authId", String.class));
        boolean valid = jwtService.isTokenValid(token, user);

        // Then
        assertThat(username).isEqualTo("john");
        assertThat(extractedAuthId).isEqualTo(user.getId().toString());
        assertThat(valid).isFalse();
    }
}
