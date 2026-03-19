package com.mobflow.userservice.services;

import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.repository.UserProfileRepository;
import com.mobflow.userservice.model.dto.request.UpdateUserProfileDTO;
import com.mobflow.userservice.exceptions.UserProfileNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile findOrCreateProfile(UUID authId, String username) {
        return userProfileRepository.findByAuthId(authId)
                .orElseGet(() -> createProfile(authId, username));
    }

    public UserProfile getProfileByAuthId(UUID authId) {
        return userProfileRepository.findByAuthId(authId)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    @Transactional
    public UserProfile updateProfile(UUID authId, UpdateUserProfileDTO dto) {
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(UserProfileNotFoundException::new);

        if (dto.getDisplayName() != null) {
            profile.setDisplayName(dto.getDisplayName());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getPhone() != null) {
            profile.setPhone(dto.getPhone());
        }

        return userProfileRepository.save(profile);
    }

    private UserProfile createProfile(UUID authId, String username) {
        UserProfile newProfile = UserProfile.createProfile(authId, username);
        return userProfileRepository.save(newProfile);
    }
}