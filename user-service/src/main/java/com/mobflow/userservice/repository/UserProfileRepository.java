package com.mobflow.userservice.repository;

import com.mobflow.userservice.model.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByAuthId(UUID authId);

    boolean existsByAuthId(UUID authId);
}
