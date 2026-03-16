package com.mobflow.authservice.model.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private UserResponseDTO user;
    private long expiresIn;

    public static LoginResponseDTO createLoginResponse(String token, UserResponseDTO user, long expiresIn) {
        return LoginResponseDTO.builder().user(user).token(token).expiresIn(expiresIn).build();
    }
}
