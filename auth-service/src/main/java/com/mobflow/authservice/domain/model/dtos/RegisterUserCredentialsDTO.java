package com.mobflow.authservice.domain.model.dtos;

public record RegisterUserCredentialsDTO(
        String username,
        String email,
        String password
) {}
