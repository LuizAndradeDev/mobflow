package com.mobflow.authservice.services;

import com.mobflow.authservice.model.dtos.request.LoginUserDTO;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserCredentialService userCredentialService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserCredentialService userCredentialService
    ) {
        this.authenticationManager = authenticationManager;
        this.userCredentialService = userCredentialService;
    }

    public UserCredential register(RegisterUserCredentialsDTO input) {
        return userCredentialService.SaveCredential(input);
    }

    public UserCredential login(LoginUserDTO input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );
        return userCredentialService.findUserCredentialByEmail(input.getEmail());
    }
}