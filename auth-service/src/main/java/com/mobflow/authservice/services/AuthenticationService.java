package com.mobflow.authservice.services;

import com.mobflow.authservice.domain.model.dtos.LoginUserDTO;
import com.mobflow.authservice.domain.model.dtos.RegisterUserCredentialsDTO;
import com.mobflow.authservice.domain.model.entities.UserCredential;
import com.mobflow.authservice.domain.model.enums.Role;
import com.mobflow.authservice.domain.repository.UserCredentialRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserCredentialRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserCredentialRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserCredential register(RegisterUserCredentialsDTO input) {
        UserCredential user = UserCredential.builder()
                .username(input.getUsername())
                .email(input.getEmail())
                .passwordHash(passwordEncoder.encode(input.getPassword()))
                .role(Role.ROLE_USER)
                .build();
        return userRepository.save(user);
    }

    public UserCredential login(LoginUserDTO input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }
}