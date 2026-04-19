package com.mobflow.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.authservice.events.AuthEventPublisher;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.mobflow.authservice.testsupport.AuthTestFixtures.loginRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.registerRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityApiTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthEventPublisher authEventPublisher;

    @BeforeEach
    void cleanUp() {
        userCredentialRepository.deleteAll();
    }

    @Test
    void signupShouldBePubliclyAccessibleWithoutAuthentication() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(registerRequest("john", "john@mobflow.dev", "Password123!"));

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void loginShouldBePubliclyAccessibleWithoutAuthentication() throws Exception {
        // Given
        persistAuthenticatableUser("john", "john@mobflow.dev", "Password123!");

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest("john@mobflow.dev", "Password123!"))))
                .andExpect(status().isOk());
    }

    @Test
    void profileShouldNotRequireAuthenticationBecauseApiAuthIsPermitAll() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("USER_NOT_FOUND"));
    }

    @Test
    void malformedBearerTokenShouldStillReachProfileAndReturnCurrentControllerFailure() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("USER_NOT_FOUND"));
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
