package com.mobflow.authservice.services;

import com.mobflow.authservice.domain.model.entities.UserCredential;
import com.mobflow.authservice.domain.repository.UserCredentialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;

    public UserCredentialService(UserCredentialRepository userCredentialRepository) {
        this.userCredentialRepository = userCredentialRepository;
    }

    public List<UserCredential> allUsers() {
        return userCredentialRepository.findAll();
    }
}
