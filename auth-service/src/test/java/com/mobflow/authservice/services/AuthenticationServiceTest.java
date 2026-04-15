package com.mobflow.authservice.services;

import com.mobflow.authservice.exceptions.GenericAplicationException;
import com.mobflow.authservice.model.dtos.request.LoginUserDTO;
import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.ErrorTP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static com.mobflow.authservice.testsupport.AuthTestFixtures.loginRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.registerRequest;
import static com.mobflow.authservice.testsupport.AuthTestFixtures.userCredential;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserCredentialService userCredentialService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager, userCredentialService);
    }

    @Test
    void registerShouldDelegateToUserCredentialService() {
        // Given
        RegisterUserCredentialsDTO request = registerRequest("john", "john@mobflow.dev", "Password123!");
        UserCredential savedUser = userCredential("john", "john@mobflow.dev", "encoded-password");

        when(userCredentialService.SaveCredential(request)).thenReturn(savedUser);

        // When
        UserCredential result = authenticationService.register(request);

        // Then
        assertThat(result).isSameAs(savedUser);
        verify(userCredentialService).SaveCredential(request);
    }

    @Test
    void loginShouldAuthenticateUsingUsernameLoadedFromEmail() {
        // Given
        LoginUserDTO request = loginRequest("john@mobflow.dev", "Password123!");
        UserCredential user = userCredential("john", "john@mobflow.dev", "encoded-password");

        when(userCredentialService.findUserCredentialByEmail(request.getEmail())).thenReturn(user);

        // When
        UserCredential result = authenticationService.login(request);

        // Then
        assertThat(result).isSameAs(user);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        UsernamePasswordAuthenticationToken authentication = authenticationCaptor.getValue();
        assertThat(authentication.getPrincipal()).isEqualTo("john");
        assertThat(authentication.getCredentials()).isEqualTo("Password123!");
    }

    @Test
    void loginShouldPropagateAuthenticationFailure() {
        // Given
        LoginUserDTO request = loginRequest("john@mobflow.dev", "wrong-password");
        UserCredential user = userCredential("john", "john@mobflow.dev", "encoded-password");

        when(userCredentialService.findUserCredentialByEmail(request.getEmail())).thenReturn(user);
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When / Then
        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginShouldPropagateUserNotFoundWhenEmailDoesNotExist() {
        // Given
        LoginUserDTO request = loginRequest("missing@mobflow.dev", "Password123!");
        when(userCredentialService.findUserCredentialByEmail(request.getEmail()))
                .thenThrow(new GenericAplicationException(ErrorTP.USER_NOT_FOUND));

        // When / Then
        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(GenericAplicationException.class)
                .hasMessage(ErrorTP.USER_NOT_FOUND.name());
    }
}
