package com.mobflow.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.services.JWTService;
import com.mobflow.authservice.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static com.mobflow.authservice.testsupport.AuthTestFixtures.loginRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.registerRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerApiTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userCredentialRepository.deleteAll();
    }

    @Test
    void postSignupShouldRegisterUserSuccessfully() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(registerRequest("john", "john@mobflow.dev", "Password123!"));

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@mobflow.dev"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void postLoginShouldReturnExpectedJsonStructure() throws Exception {
        // Given
        persistAuthenticatableUser("john", "john@mobflow.dev", "Password123!");

        String requestBody = objectMapper.writeValueAsString(loginRequest("john@mobflow.dev", "Password123!"));

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("john"))
                .andExpect(jsonPath("$.user.email").value("john@mobflow.dev"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void postSignupShouldReturnConflictForDuplicateUsername() throws Exception {
        // Given
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("john", "john@mobflow.dev", "Password123!"))))
                .andExpect(status().isOk());

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("john", "other@mobflow.dev", "Password123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("USERNAME_ALREADY_EXIST"));
    }

    @Test
    void postSignupShouldReturnConflictForDuplicateEmail() throws Exception {
        // Given
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("john", "john@mobflow.dev", "Password123!"))))
                .andExpect(status().isOk());

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("other", "john@mobflow.dev", "Password123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("EMAIL_ALREADY_EXIST"));
    }

    @Test
    void postLoginShouldReturnUnauthorizedForBadCredentials() throws Exception {
        // Given
        persistAuthenticatableUser("john", "john@mobflow.dev", "Password123!");

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("john@mobflow.dev", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getProfileShouldReturnAuthenticatedUserWhenBearerTokenIsValid() throws Exception {
        // Given
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("john", "john@mobflow.dev", "Password123!"))))
                .andExpect(status().isOk());
        UserCredential persistedUser = userCredentialRepository.findByUsername("john").orElseThrow();
        String token = jwtService.generateToken(persistedUser);

        // When / Then
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@mobflow.dev"));
    }

    private UserCredential persistAuthenticatableUser(String username, String email, String rawPassword) {
        UserCredential user = UserCredential.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ROLE_USER)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        return userCredentialRepository.save(user);
    }
}
