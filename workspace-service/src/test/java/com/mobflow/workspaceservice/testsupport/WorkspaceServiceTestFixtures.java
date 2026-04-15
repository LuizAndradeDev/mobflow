package com.mobflow.workspaceservice.testsupport;

import com.mobflow.workspaceservice.model.dto.request.AddMemberByUsernameDTO;
import com.mobflow.workspaceservice.model.dto.request.CreateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateMemberRoleDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateWorkspaceDTO;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.InviteStatus;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

public final class WorkspaceServiceTestFixtures {

    private WorkspaceServiceTestFixtures() {
    }

    public static Workspace workspace(UUID ownerAuthId) {
        return Workspace.builder()
                .id(UUID.randomUUID())
                .publicCode("ABC12345")
                .name("Mobflow")
                .description("Workspace")
                .ownerAuthId(ownerAuthId)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static WorkspaceMember workspaceMember(Workspace workspace, UUID authId, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .authId(authId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    public static WorkspaceInvite workspaceInvite(Workspace workspace, UUID targetAuthId, UUID invitedByAuthId, InviteStatus status) {
        return WorkspaceInvite.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .targetAuthId(targetAuthId)
                .invitedByAuthId(invitedByAuthId)
                .status(status)
                .createdAt(LocalDateTime.now().minusHours(1))
                .respondedAt(status == InviteStatus.PENDING ? null : LocalDateTime.now())
                .build();
    }

    public static CreateWorkspaceDTO createWorkspaceDTO() {
        CreateWorkspaceDTO dto = new CreateWorkspaceDTO();
        dto.setName("Mobflow");
        dto.setDescription("Workspace");
        return dto;
    }

    public static UpdateWorkspaceDTO updateWorkspaceDTO() {
        UpdateWorkspaceDTO dto = new UpdateWorkspaceDTO();
        dto.setName("Updated Workspace");
        dto.setDescription("Updated Description");
        return dto;
    }

    public static AddMemberByUsernameDTO addMemberByUsernameDTO(String username) {
        AddMemberByUsernameDTO dto = new AddMemberByUsernameDTO();
        dto.setUsername(username);
        return dto;
    }

    public static UpdateMemberRoleDTO updateMemberRoleDTO(WorkspaceRole role) {
        UpdateMemberRoleDTO dto = new UpdateMemberRoleDTO();
        dto.setRole(role);
        return dto;
    }
}
