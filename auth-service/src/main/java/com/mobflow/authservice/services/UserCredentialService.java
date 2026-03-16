package com.mobflow.authservice.services;

import com.mobflow.authservice.model.dtos.request.RegisterUserCredentialsDTO;
import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.ErrorTP;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.exceptions.GenericAplicationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCredentialService {
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
        if (userCredentialRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new GenericAplicationException(ErrorTP.USERNAME_ALREADY_EXIST);
        }
        if (userCredentialRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new GenericAplicationException(ErrorTP.EMAIL_ALREADY_EXIST);
        }
        UserCredential userCredential = UserCredential.createUserCredential(
                user.getUsername(), user.getEmail(), passwordEncoder.encode(user.getPassword())
        );
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
}
