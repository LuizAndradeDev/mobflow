package com.mobflow.workspaceservice.controller;

import com.mobflow.workspaceservice.model.dto.request.AddMemberDTO;
import com.mobflow.workspaceservice.model.dto.request.CreateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateMemberRoleDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.response.WorkspaceMemberResponseDTO;
import com.mobflow.workspaceservice.model.dto.response.WorkspaceResponseDTO;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponseDTO> create(
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceDTO dto
    ) {
        UUID authId = extractAuthId(authentication);
        Workspace workspace = workspaceService.createWorkspace(dto, authId);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceResponseDTO.fromEntity(workspace));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponseDTO>> listMine(Authentication authentication) {
        UUID authId = extractAuthId(authentication);
        List<WorkspaceResponseDTO> workspaces = workspaceService.listMyWorkspaces(authId)
                .stream()
                .map(WorkspaceResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(workspaces);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> getById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID authId = extractAuthId(authentication);
        Workspace workspace = workspaceService.getWorkspaceById(id, authId);
        return ResponseEntity.ok(WorkspaceResponseDTO.fromEntity(workspace));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceDTO dto
    ) {
        UUID authId = extractAuthId(authentication);
        Workspace workspace = workspaceService.updateWorkspace(id, dto, authId);
        return ResponseEntity.ok(WorkspaceResponseDTO.fromEntity(workspace));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID authId = extractAuthId(authentication);
        workspaceService.deleteWorkspace(id, authId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberResponseDTO> addMember(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberDTO dto
    ) {
        UUID authId = extractAuthId(authentication);
        WorkspaceMember member = workspaceService.addMember(id, dto, authId);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceMemberResponseDTO.fromEntity(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberResponseDTO>> listMembers(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID authId = extractAuthId(authentication);
        List<WorkspaceMemberResponseDTO> members = workspaceService.listMembers(id, authId)
                .stream()
                .map(WorkspaceMemberResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{id}/members/{memberAuthId}")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID memberAuthId
    ) {
        UUID authId = extractAuthId(authentication);
        workspaceService.removeMember(id, memberAuthId, authId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{memberAuthId}/role")
    public ResponseEntity<WorkspaceMemberResponseDTO> updateMemberRole(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID memberAuthId,
            @Valid @RequestBody UpdateMemberRoleDTO dto
    ) {
        UUID authId = extractAuthId(authentication);
        WorkspaceMember member = workspaceService.updateMemberRole(id, memberAuthId, dto, authId);
        return ResponseEntity.ok(WorkspaceMemberResponseDTO.fromEntity(member));
    }

    private UUID extractAuthId(Authentication authentication) {
        return (UUID) authentication.getCredentials();
    }
}
