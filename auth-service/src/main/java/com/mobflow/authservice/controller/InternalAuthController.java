package com.mobflow.authservice.controller;

import com.mobflow.authservice.model.dtos.response.InternalUserLookupResponseDTO;
import com.mobflow.authservice.services.UserCredentialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/internal/auth/users")
public class InternalAuthController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final UserCredentialService userCredentialService;
    private final String internalSecret;

    public InternalAuthController(
            UserCredentialService userCredentialService,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.userCredentialService = userCredentialService;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<InternalUserLookupResponseDTO> getByUsername(
            @PathVariable String username,
            @RequestHeader(value = INTERNAL_SECRET_HEADER, required = false) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userCredentialService.getUserByUsername(username));
    }

    @PostMapping("/resolve")
    public ResponseEntity<List<InternalUserLookupResponseDTO>> resolveByUsernames(
            @RequestBody List<String> usernames,
            @RequestHeader(value = INTERNAL_SECRET_HEADER, required = false) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userCredentialService.resolveUsersByUsername(usernames));
    }

    private boolean hasValidSecret(String secret) {
        return secret != null && MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
