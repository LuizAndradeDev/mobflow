package com.mobflow.authservice.controller;

import com.mobflow.authservice.model.dtos.request.LoginUserDTO;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.dtos.response.LoginResponseDTO;
import com.mobflow.authservice.model.dtos.response.UserResponseDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.services.AuthenticationService;
import com.mobflow.authservice.services.JWTService;
import com.mobflow.authservice.services.UserCredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JWTService jwtService;
    private final UserCredentialService userCredentialService;
    private final AuthenticationService authenticationService;

    public AuthController(
            JWTService jwtService,
            AuthenticationService authenticationService,
            UserCredentialService userCredentialService
    ) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.userCredentialService = userCredentialService;

    }

    @PostMapping("/signup")
    public ResponseEntity<UserCredential> register(@RequestBody RegisterUserCredentialsDTO registerUserDto) {
        UserCredential registeredUser = authenticationService.register(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        authenticationService.confirmEmail(token);
        return ResponseEntity.ok("Email confirmed successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@RequestBody LoginUserDTO loginUserDto) {
        UserCredential authenticatedUser = authenticationService.login(loginUserDto);

        UserResponseDTO userResponse = UserResponseDTO.createUserResponse(authenticatedUser.getUsername(), authenticatedUser.getEmail());

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponseDTO loginResponse = LoginResponseDTO.createLoginResponse(jwtToken, userResponse, jwtService.getExpirationTime());

        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserCredential userCredential = userCredentialService.findUserCredentialByUsername(authentication.getName());

        UserResponseDTO user = UserResponseDTO.createUserResponse(userCredential.getUsername(), userCredential.getEmail());

        return ResponseEntity.ok(user);
    }
}
