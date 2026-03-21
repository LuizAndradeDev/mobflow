package com.mobflow.userservice.controller;

import com.mobflow.userservice.model.dto.response.BatchUserResponseDTO;
import com.mobflow.userservice.model.dto.response.UserProfileResponseDTO;
import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.repository.UserProfileRepository;
import com.mobflow.userservice.services.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final String internalSecret;
    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;

    public InternalUserController(
            UserProfileService userProfileService,
            UserProfileRepository userProfileRepository,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.userProfileService = userProfileService;
        this.userProfileRepository = userProfileRepository;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserProfileResponseDTO> getByUsername(
            @PathVariable String username,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(403).build();
        }
        UserProfileResponseDTO profile = userProfileService.getProfileByUsername(username);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<BatchUserResponseDTO>> getBatch(
            @RequestBody List<UUID> authIds,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(403).build();
        }

        List<BatchUserResponseDTO> result = userProfileRepository
                .findAllByAuthIdIn(authIds)
                .stream()
                .map(p -> BatchUserResponseDTO.builder()
                        .authId(p.getAuthId())
                        .displayName(p.getDisplayName())
                        .avatarUrl(p.getAvatarUrl())
                        .build())
                .toList();

        return ResponseEntity.ok(result);
    }
}
