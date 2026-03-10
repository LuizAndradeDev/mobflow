package com.mobflow.authservice.domain.model.dtos;

import lombok.Data;

@Data
public class RegisterUserCredentialsDTO{
        private String username;
        private String email;
        private String password;
}
