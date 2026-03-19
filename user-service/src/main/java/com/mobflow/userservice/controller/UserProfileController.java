package com.mobflow.userservice.controller;

import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.model.dto.request.UpdateUserProfileDTO;
import com.mobflow.userservice.model.dto.response.UserProfileResponseDTO;
import com.mobflow.userservice.services.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDTO> getMyProfile(Authentication authentication) {
        UUID authId = (UUID) authentication.getCredentials();
        String username = (String) authentication.getPrincipal();

        UserProfile profile = userProfileService.findOrCreateProfile(authId, username);
        return ResponseEntity.ok(UserProfileResponseDTO.fromEntity(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponseDTO> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileDTO dto
    ) {
        UUID authId = (UUID) authentication.getCredentials();

        UserProfile updated = userProfileService.updateProfile(authId, dto);
        return ResponseEntity.ok(UserProfileResponseDTO.fromEntity(updated));
    }

    @PatchMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponseDTO> updateAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        UUID authId = (UUID) authentication.getCredentials();

        UserProfile updated = userProfileService.updateAvatar(authId, file);
        return ResponseEntity.ok(UserProfileResponseDTO.fromEntity(updated));
    }

    @GetMapping("/{authId}")
    public ResponseEntity<UserProfileResponseDTO> getProfileByAuthId(
            @PathVariable UUID authId
    ) {
        UserProfile profile = userProfileService.getProfileByAuthId(authId);
        return ResponseEntity.ok(UserProfileResponseDTO.fromEntity(profile));
    }
}
