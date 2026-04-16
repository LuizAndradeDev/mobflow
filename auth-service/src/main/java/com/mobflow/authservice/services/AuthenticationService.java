package com.mobflow.authservice.services;

import com.mobflow.authservice.events.AuthEventPublisher;
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
    private final AuthEventPublisher authEventPublisher;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserCredentialService userCredentialService,
            AuthEventPublisher authEventPublisher
    ) {
        this.authenticationManager = authenticationManager;
        this.userCredentialService = userCredentialService;
        this.authEventPublisher = authEventPublisher;
    }

    public UserCredential register(RegisterUserCredentialsDTO input) {
        UserCredential createdCredential = userCredentialService.SaveCredential(input);
        authEventPublisher.publishEmailConfirmation(createdCredential);
        return createdCredential;
    }

    public UserCredential login(LoginUserDTO input) {
        UserCredential userCredential = userCredentialService.findUserCredentialByEmail(input.getEmail());
        authenticate(input, userCredential);
        return userCredential;
    }

    private void authenticate(LoginUserDTO input, UserCredential userCredential) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userCredential.getUsername(),
                        input.getPassword()
                )
        );
    }

    public UserCredential confirmEmail(String token) {
        return userCredentialService.confirmEmail(token);
    }
}
