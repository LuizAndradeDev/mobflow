package com.mobflow.authservice.domain.model.dtos;

import lombok.Data;

@Data
public class LoginUserDTO {
    private String email;
    private String password;
}