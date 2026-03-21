package com.mobflow.userservice.controller;

import com.mobflow.userservice.model.dto.response.BatchUserResponseDTO;
import com.mobflow.userservice.model.dto.response.UserProfileResponseDTO;
import com.mobflow.userservice.model.entities.UserProfile;
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

    public InternalUserController(
            UserProfileService userProfileService,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.userProfileService = userProfileService;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserProfileResponseDTO> getByUsername(
            @PathVariable String username,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) return ResponseEntity.status(403).build();

        UserProfileResponseDTO profile = userProfileService.getProfileByUsername(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * Batch endpoint: receives a list of authIds and returns profiles.
     * Each individual profile lookup goes through @Cacheable in UserProfileService,
     * so repeated calls for the same authId are served from Redis without hitting PostgreSQL.
     *
     * Flow:
     *   workspace-service calls this with [uuid1, uuid2, uuid3]
     *   → for each uuid: check Redis → HIT: return cached / MISS: query DB, cache, return
     *   → total DB queries = only for uncached profiles
     */
    @PostMapping("/batch")
    public ResponseEntity<List<BatchUserResponseDTO>> getBatch(
            @RequestBody List<UUID> authIds,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) return ResponseEntity.status(403).build();

        List<BatchUserResponseDTO> result = authIds.stream()
                .map(authId -> {
                    try {
                        UserProfile profile = userProfileService.getProfileByAuthId(authId);
                        return BatchUserResponseDTO.builder()
                                .authId(profile.getAuthId())
                                .displayName(profile.getDisplayName())
                                .avatarUrl(profile.getAvatarUrl())
                                .build();
                    } catch (Exception e) {
                        // profile not found — return minimal fallback so member list still renders
                        return BatchUserResponseDTO.builder()
                                .authId(authId)
                                .displayName(authId.toString().substring(0, 8))
                                .avatarUrl(null)
                                .build();
                    }
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
