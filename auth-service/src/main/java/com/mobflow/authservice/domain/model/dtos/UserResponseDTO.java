package com.mobflow.authservice.domain.model.dtos;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
}