package com.mobflow.userservice.services;

import com.mobflow.userservice.model.dto.request.UpdateUserProfileDTO;
import com.mobflow.userservice.model.dto.response.BatchUserResponseDTO;
import com.mobflow.userservice.model.dto.response.UserProfileResponseDTO;
import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.exceptions.UserProfileNotFoundException;
import com.mobflow.userservice.repository.UserProfileRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final StorageService storageService;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            StorageService storageService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.storageService = storageService;
    }


    @Transactional
    public UserProfile findOrCreateProfile(UUID authId, String username) {
        return userProfileRepository.findByAuthId(authId)
                .orElseGet(() -> createProfile(authId, username));
    }

    @Cacheable(value = "user-profiles", key = "#authId")
    public UserProfile getProfileByAuthId(UUID authId) {
        return userProfileRepository.findByAuthId(authId)
                .orElseThrow(UserProfileNotFoundException::new);
    }


    public UserProfileResponseDTO getProfileByUsername(String username) {
        return UserProfileResponseDTO.fromEntity(getRequiredProfileByDisplayName(username));
    }

    public List<BatchUserResponseDTO> getBatchProfiles(List<UUID> authIds) {
        return authIds.stream()
                .map(this::buildBatchProfile)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "user-profiles", key = "#authId")
    public UserProfile updateProfile(UUID authId, UpdateUserProfileDTO dto) {
        UserProfile profile = getRequiredProfileByAuthId(authId);
        applyProfileUpdates(profile, dto);

        return userProfileRepository.save(profile);
    }

    @Transactional
    @CacheEvict(value = "user-profiles", key = "#authId")
    public UserProfile updateAvatar(UUID authId, MultipartFile file) {
        UserProfile profile = getRequiredProfileByAuthId(authId);
        deleteCurrentAvatar(profile);

        String avatarUrl = storageService.uploadAvatar(file);
        profile.setAvatarUrl(avatarUrl);

        return userProfileRepository.save(profile);
    }

    private UserProfile createProfile(UUID authId, String username) {
        UserProfile newProfile = UserProfile.createProfile(authId, username);
        return userProfileRepository.save(newProfile);
    }

    private UserProfile getRequiredProfileByAuthId(UUID authId) {
        return userProfileRepository.findByAuthId(authId)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    private UserProfile getRequiredProfileByDisplayName(String displayName) {
        return userProfileRepository.findByDisplayName(displayName)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    private void applyProfileUpdates(UserProfile profile, UpdateUserProfileDTO dto) {
        if (dto.getDisplayName() != null) {
            profile.setDisplayName(dto.getDisplayName());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getPhone() != null) {
            profile.setPhone(dto.getPhone());
        }
    }

    private void deleteCurrentAvatar(UserProfile profile) {
        if (profile.getAvatarUrl() != null) {
            storageService.deleteAvatar(profile.getAvatarUrl());
        }
    }

    private BatchUserResponseDTO buildBatchProfile(UUID authId) {
        try {
            UserProfile profile = getProfileByAuthId(authId);
            return BatchUserResponseDTO.builder()
                    .authId(profile.getAuthId())
                    .displayName(profile.getDisplayName())
                    .avatarUrl(profile.getAvatarUrl())
                    .build();
        } catch (UserProfileNotFoundException exception) {
            return BatchUserResponseDTO.builder()
                    .authId(authId)
                    .displayName(authId.toString().substring(0, 8))
                    .avatarUrl(null)
                    .build();
        }
    }
}
