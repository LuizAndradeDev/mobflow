package com.mobflow.authservice.testsupport;

import com.mobflow.authservice.model.dtos.request.LoginUserDTO;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;

import java.util.UUID;

public final class AuthTestFixtures {

    private AuthTestFixtures() {
    }

    public static RegisterUserCredentialsDTO registerRequest(String username, String email, String password) {
        RegisterUserCredentialsDTO request = new RegisterUserCredentialsDTO();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    public static LoginUserDTO loginRequest(String email, String password) {
        LoginUserDTO request = new LoginUserDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    public static UserCredential userCredential(String username, String email, String passwordHash) {
        return userCredential(username, email, passwordHash, Role.ROLE_USER);
    }

    public static UserCredential userCredential(String username, String email, String passwordHash, Role role) {
        return UserCredential.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
    }
}
