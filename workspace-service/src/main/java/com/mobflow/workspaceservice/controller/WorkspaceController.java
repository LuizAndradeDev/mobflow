package com.mobflow.workspaceservice.controller;

import com.mobflow.workspaceservice.model.dto.request.AddMemberByUsernameDTO;
import com.mobflow.workspaceservice.model.dto.request.CreateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateMemberRoleDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.response.WorkspaceMemberResponseDTO;
import com.mobflow.workspaceservice.model.dto.response.WorkspaceMemberWithProfileDTO;
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
        Workspace workspace = workspaceService.createWorkspace(dto, extractAuthId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceResponseDTO.fromEntity(workspace));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponseDTO>> listMine(Authentication authentication) {
        return ResponseEntity.ok(
                workspaceService.listMyWorkspaces(extractAuthId(authentication))
                        .stream().map(WorkspaceResponseDTO::fromEntity).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> getById(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(
                WorkspaceResponseDTO.fromEntity(workspaceService.getWorkspaceById(id, extractAuthId(authentication)))
        );
    }

    @GetMapping("/join/{code}")
    public ResponseEntity<WorkspaceResponseDTO> previewByCode(@PathVariable String code) {
        return ResponseEntity.ok(WorkspaceResponseDTO.fromEntity(workspaceService.getWorkspaceByCode(code)));
    }

    @PostMapping("/join/{code}")
    public ResponseEntity<WorkspaceMemberResponseDTO> joinByCode(Authentication authentication, @PathVariable String code) {
        WorkspaceMember member = workspaceService.joinByCode(code, extractAuthId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceMemberResponseDTO.fromEntity(member));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDTO> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceDTO dto
    ) {
        return ResponseEntity.ok(
                WorkspaceResponseDTO.fromEntity(workspaceService.updateWorkspace(id, dto, extractAuthId(authentication)))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        workspaceService.deleteWorkspace(id, extractAuthId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leave(Authentication authentication, @PathVariable UUID id) {
        workspaceService.leaveWorkspace(id, extractAuthId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberWithProfileDTO> addMember(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberByUsernameDTO dto
    ) {
        WorkspaceMember member = workspaceService.addMemberByUsername(id, dto, extractAuthId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkspaceMemberWithProfileDTO.fromMember(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberWithProfileDTO>> listMembers(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(workspaceService.listMembersWithProfiles(id, extractAuthId(authentication)));
    }

    @DeleteMapping("/{id}/members/{memberAuthId}")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID memberAuthId
    ) {
        workspaceService.removeMember(id, memberAuthId, extractAuthId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{memberAuthId}/role")
    public ResponseEntity<WorkspaceMemberResponseDTO> updateMemberRole(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID memberAuthId,
            @Valid @RequestBody UpdateMemberRoleDTO dto
    ) {
        WorkspaceMember member = workspaceService.updateMemberRole(id, memberAuthId, dto, extractAuthId(authentication));
        return ResponseEntity.ok(WorkspaceMemberResponseDTO.fromEntity(member));
    }

    private UUID extractAuthId(Authentication authentication) {
        return (UUID) authentication.getCredentials();
    }
}
