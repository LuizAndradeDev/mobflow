package com.mobflow.authservice.model.dtos.request;

import lombok.Data;

@Data
public class RegisterUserCredentialsDTO{
        private String username;
        private String email;
        private String password;
}
