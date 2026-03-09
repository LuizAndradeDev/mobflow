package com.mobflow.authservice.domain.repository;

import com.mobflow.authservice.domain.model.entities.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    Optional<UserCredential> findByUsername(String username);
    Optional<UserCredential> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
