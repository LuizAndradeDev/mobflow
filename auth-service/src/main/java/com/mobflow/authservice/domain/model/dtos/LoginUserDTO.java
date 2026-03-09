package com.mobflow.authservice.domain.model.dtos;

public record LoginUserDTO(
        String login,
        String password
) {}