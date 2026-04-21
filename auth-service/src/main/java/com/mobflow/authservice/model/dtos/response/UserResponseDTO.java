package com.mobflow.authservice.model.dtos.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;

    public static UserResponseDTO createUserResponse(String username, String email) {
        return UserResponseDTO.builder().username(username).email(email).build();
    }
}
