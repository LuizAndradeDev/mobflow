package com.mobflow.chatservice.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public record AuthenticatedUser(
        UUID authId,
        String username
) {
    public static AuthenticatedUser from(Authentication authentication) {
        return new AuthenticatedUser(
                (UUID) authentication.getCredentials(),
                (String) authentication.getPrincipal()
        );
    }

    public static AuthenticatedUser of(UUID authId, String username) {
        return new AuthenticatedUser(authId, username);
    }
}
