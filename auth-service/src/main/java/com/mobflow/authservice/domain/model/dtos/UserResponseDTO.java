package com.mobflow.authservice.domain.model.dtos;

public record UserResponseDTO(
    Long id,
    String username,
    String email
){}
