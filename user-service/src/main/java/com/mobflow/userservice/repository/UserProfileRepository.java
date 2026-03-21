package com.mobflow.userservice.repository;

import com.mobflow.userservice.model.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByAuthId(UUID authId);

    Optional<UserProfile> findByDisplayName(String displayName);

    List<UserProfile> findAllByAuthIdIn(List<UUID> authIds);

    boolean existsByAuthId(UUID authId);
}
