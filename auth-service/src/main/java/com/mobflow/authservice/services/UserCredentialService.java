package com.mobflow.authservice.services;

import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.ErrorTP;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.exceptions.GenericAplicationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserCredentialService {
    private static final long CONFIRMATION_TOKEN_TTL_HOURS = 24;

    private final PasswordEncoder passwordEncoder;
    private final UserCredentialRepository userCredentialRepository;

    public UserCredentialService(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserCredential SaveCredential(RegisterUserCredentialsDTO user) {
        ensureUsernameIsAvailable(user.getUsername());
        ensureEmailIsAvailable(user.getEmail());
        UserCredential userCredential = UserCredential.createUserCredential(
                user.getUsername(), user.getEmail(), passwordEncoder.encode(user.getPassword())
        );
        userCredential.setConfirmationToken(UUID.randomUUID().toString());
        userCredential.setConfirmationTokenExpiresAt(LocalDateTime.now().plusHours(CONFIRMATION_TOKEN_TTL_HOURS));
        return userCredentialRepository.save(userCredential);
    }

    public UserCredential findUserCredentialByEmail(String email) {
        return userCredentialRepository.findByEmail(email).orElseThrow(() -> new GenericAplicationException(ErrorTP.USER_NOT_FOUND));
    }

    public UserCredential findUserCredentialByUsername(String username) {
        return userCredentialRepository.findByUsername(username).orElseThrow(() -> new GenericAplicationException(ErrorTP.USER_NOT_FOUND));
    }

    public List<UserCredential> allUsers() {
        return userCredentialRepository.findAll();
    }

    public UserCredential confirmEmail(String confirmationToken) {
        UserCredential userCredential = userCredentialRepository.findByConfirmationToken(confirmationToken)
                .orElseThrow(() -> new GenericAplicationException(ErrorTP.USER_NOT_FOUND));

        if (isConfirmationTokenExpired(userCredential)) {
            throw new GenericAplicationException(ErrorTP.GENERIC_ERROR);
        }

        userCredential.setEnabled(true);
        userCredential.setEmailConfirmedAt(LocalDateTime.now());
        userCredential.setConfirmationToken(null);
        userCredential.setConfirmationTokenExpiresAt(null);
        return userCredentialRepository.save(userCredential);
    }

    private void ensureUsernameIsAvailable(String username) {
        if (userCredentialRepository.findByUsername(username).isPresent()) {
            throw new GenericAplicationException(ErrorTP.USERNAME_ALREADY_EXIST);
        }
    }

    private void ensureEmailIsAvailable(String email) {
        if (userCredentialRepository.findByEmail(email).isPresent()) {
            throw new GenericAplicationException(ErrorTP.EMAIL_ALREADY_EXIST);
        }
    }

    private boolean isConfirmationTokenExpired(UserCredential userCredential) {
        return userCredential.getConfirmationTokenExpiresAt() == null
                || userCredential.getConfirmationTokenExpiresAt().isBefore(LocalDateTime.now());
    }
}
