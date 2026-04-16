package com.mobflow.userservice.controller;

import com.mobflow.userservice.model.dto.response.BatchUserResponseDTO;
import com.mobflow.userservice.model.dto.response.UserProfileResponseDTO;
import com.mobflow.userservice.services.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userProfileService.getProfileByUsername(username));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<BatchUserResponseDTO>> getBatch(
            @RequestBody List<UUID> authIds,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userProfileService.getBatchProfiles(authIds));
    }

    private boolean hasValidSecret(String secret) {
        return internalSecret.equals(secret);
    }
}
