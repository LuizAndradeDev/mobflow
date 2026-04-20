package com.mobflow.workspaceservice.controller;

import com.mobflow.workspaceservice.model.dto.response.MemberRoleResponseDTO;
import com.mobflow.workspaceservice.repository.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/internal/workspaces")
public class InternalWorkspaceController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final String internalSecret;

    public InternalWorkspaceController(
            WorkspaceMemberRepository workspaceMemberRepository,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/{workspaceId}/members/{authId}/role")
    public ResponseEntity<MemberRoleResponseDTO> getMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID authId,
            @RequestHeader(value = INTERNAL_SECRET_HEADER, required = false) String secret
    ) {
        if (!hasValidSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, authId)
                .map(member -> ResponseEntity.ok(MemberRoleResponseDTO.of(member.getRole().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean hasValidSecret(String secret) {
        return secret != null && MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
