package com.mobflow.authservice.integration;

import com.mobflow.authservice.exceptions.GenericAplicationException;
import com.mobflow.authservice.model.dtos.request.LoginUserDTO;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.ErrorTP;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.services.AuthenticationService;
import com.mobflow.authservice.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.mobflow.authservice.testsupport.AuthTestFixtures.loginRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.registerRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userCredentialRepository.deleteAll();
    }

    @Test
    void contextShouldStartWithRealInfrastructureBeans() {
        // Then
        assertThat(authenticationService).isNotNull();
        assertThat(userCredentialRepository).isNotNull();
    }

    @Test
    void shouldSaveAndLoadUserFromRealDatabase() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");

        // When
        UserCredential registeredUser = authenticationService.register(request);
        UserCredential reloadedUser = userCredentialRepository.findById(registeredUser.getId()).orElseThrow();

        // Then
        assertThat(reloadedUser.getUsername()).isEqualTo("john");
        assertThat(reloadedUser.getEmail()).isEqualTo("john@mobflow.dev");
        assertThat(reloadedUser.getPasswordHash()).isNotEqualTo("Password123!");
    }

    @Test
    void shouldRegisterUserThroughServiceLayerUsingRealDatabase() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");

        // When
        UserCredential registeredUser = authenticationService.register(request);

        // Then
        assertThat(registeredUser.getId()).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo("john");
        assertThat(registeredUser.getEmail()).isEqualTo("john@mobflow.dev");
        assertThat(registeredUser.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(userCredentialRepository.findByUsername("john")).isPresent();
    }

    @Test
    void shouldLoginUserThroughServiceLayerUsingRealDatabase() {
        // Given
        persistAuthenticatableUser("john", "john@mobflow.dev", "Password123!");
        LoginUserDTO loginRequest = loginRequest("john@mobflow.dev", "Password123!");

        // When
        UserCredential authenticatedUser = authenticationService.login(loginRequest);

        // Then
        assertThat(authenticatedUser.getUsername()).isEqualTo("john");
        assertThat(authenticatedUser.getEmail()).isEqualTo("john@mobflow.dev");
    }

    @Test
    void shouldRejectDuplicateUsernameThroughServiceLayer() {
        // Given
        authenticationService.register(registerRequest("john", "john@mobflow.dev", "Password123!"));

        // When / Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest("john", "other@mobflow.dev", "Password123!")))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.USERNAME_ALREADY_EXIST.name());
    }

    @Test
    void shouldRejectDuplicateEmailThroughServiceLayer() {
        // Given
        authenticationService.register(registerRequest("john", "john@mobflow.dev", "Password123!"));

        // When / Then
        assertThatThrownBy(() -> authenticationService.register(registerRequest("other", "john@mobflow.dev", "Password123!")))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.EMAIL_ALREADY_EXIST.name());
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
