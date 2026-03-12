package com.mobflow.authservice.controller;

import com.mobflow.authservice.domain.model.dtos.LoginUserDTO;
import com.mobflow.authservice.domain.model.dtos.RegisterUserCredentialsDTO;
import com.mobflow.authservice.domain.model.dtos.UserResponseDTO;
import com.mobflow.authservice.domain.model.entities.UserCredential;
import com.mobflow.authservice.domain.repository.UserCredentialRepository;
import com.mobflow.authservice.domain.responses.LoginResponse;
import com.mobflow.authservice.services.AuthenticationService;
import com.mobflow.authservice.services.JWTService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JWTService jwtService;
    private final UserCredentialRepository repository;
    private final AuthenticationService authenticationService;

    public AuthController(JWTService jwtService, AuthenticationService authenticationService, UserCredentialRepository repository) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.repository = repository;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserCredential> register(@RequestBody RegisterUserCredentialsDTO registerUserDto) {
        UserCredential registeredUser = authenticationService.register(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDTO loginUserDto) {
        UserCredential authenticatedUser = authenticationService.login(loginUserDto);

        UserResponseDTO user = UserResponseDTO.builder()
                .email(authenticatedUser.getEmail())
                .username(authenticatedUser.getUsername())
                .build();

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken)
                .user(user)
                .expiresIn(jwtService.getExpirationTime())
                .build();

        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserCredential userEntity = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserResponseDTO user = UserResponseDTO.builder()
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .build();

        return ResponseEntity.ok(user);
    }
}
