package com.mobflow.authservice.domain.responses;

import com.mobflow.authservice.domain.model.dtos.UserResponseDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserResponseDTO user;
    private long expiresIn;
}
