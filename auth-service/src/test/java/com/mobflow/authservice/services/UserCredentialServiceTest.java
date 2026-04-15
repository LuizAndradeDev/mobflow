package com.mobflow.authservice.services;

import com.mobflow.authservice.exceptions.GenericAplicationException;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.ErrorTP;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.mobflow.authservice.testsupport.AuthTestFixtures.registerRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.userCredential;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserCredentialService userCredentialService;

    @BeforeEach
    void setUp() {
        userCredentialService = new UserCredentialService(userCredentialRepository, passwordEncoder);
    }

    @Test
    void saveCredentialShouldRegisterUserWithEncodedPasswordAndDefaultRole() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");

        when(userCredentialRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userCredentialRepository.findByEmail("john@mobflow.dev")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
        when(userCredentialRepository.save(any(UserCredential.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserCredential saved = userCredentialService.SaveCredential(request);

        // Then
        ArgumentCaptor<UserCredential> credentialCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credentialCaptor.capture());

        UserCredential persistedUser = credentialCaptor.getValue();
        assertThat(saved).isSameAs(persistedUser);
        assertThat(persistedUser.getUsername()).isEqualTo("john");
        assertThat(persistedUser.getEmail()).isEqualTo("john@mobflow.dev");
        assertThat(persistedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(persistedUser.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void saveCredentialShouldRejectDuplicateUsername() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");
        when(userCredentialRepository.findByUsername("john"))
                .thenReturn(Optional.of(userCredential("john", "existing@mobflow.dev", "hash")));

        // When / Then
        assertThatThrownBy(() -> userCredentialService.SaveCredential(request))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.USERNAME_ALREADY_EXIST.name());

        verify(userCredentialRepository, never()).save(any());
    }

    @Test
    void saveCredentialShouldRejectDuplicateEmail() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");

        when(userCredentialRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userCredentialRepository.findByEmail("john@mobflow.dev"))
                .thenReturn(Optional.of(userCredential("existing-user", "john@mobflow.dev", "hash")));

        // When / Then
        assertThatThrownBy(() -> userCredentialService.SaveCredential(request))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.EMAIL_ALREADY_EXIST.name());

        verify(userCredentialRepository, never()).save(any());
    }

    @Test
    void findUserCredentialByEmailShouldReturnUserWhenPresent() {
        // Given
        UserCredential user = userCredential("john", "john@mobflow.dev", "encoded-password");
        when(userCredentialRepository.findByEmail("john@mobflow.dev")).thenReturn(Optional.of(user));

        // When
        UserCredential result = userCredentialService.findUserCredentialByEmail("john@mobflow.dev");

        // Then
        assertThat(result).isSameAs(user);
    }

    @Test
    void findUserCredentialByEmailShouldThrowWhenMissing() {
        // Given
        when(userCredentialRepository.findByEmail("missing@mobflow.dev")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userCredentialService.findUserCredentialByEmail("missing@mobflow.dev"))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.USER_NOT_FOUND.name());
    }

    @Test
    void findUserCredentialByUsernameShouldReturnUserWhenPresent() {
        // Given
        UserCredential user = userCredential("john", "john@mobflow.dev", "encoded-password");
        when(userCredentialRepository.findByUsername("john")).thenReturn(Optional.of(user));

        // When
        UserCredential result = userCredentialService.findUserCredentialByUsername("john");

        // Then
        assertThat(result).isSameAs(user);
    }

    @Test
    void findUserCredentialByUsernameShouldThrowWhenMissing() {
        // Given
        when(userCredentialRepository.findByUsername("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userCredentialService.findUserCredentialByUsername("missing"))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.USER_NOT_FOUND.name());
    }
}
