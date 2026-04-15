package com.mobflow.workspaceservice.service;

import com.mobflow.workspaceservice.config.UserServiceClient;
import com.mobflow.workspaceservice.events.WorkspaceEventPublisher;
import com.mobflow.workspaceservice.exception.CannotRemoveOwnerException;
import com.mobflow.workspaceservice.exception.MemberAlreadyExistsException;
import com.mobflow.workspaceservice.exception.MemberNotFoundException;
import com.mobflow.workspaceservice.exception.UnauthorizedWorkspaceActionException;
import com.mobflow.workspaceservice.model.dto.request.AddMemberByUsernameDTO;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.InviteStatus;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.repository.WorkspaceInviteRepository;
import com.mobflow.workspaceservice.repository.WorkspaceMemberRepository;
import com.mobflow.workspaceservice.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.addMemberByUsernameDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.createWorkspaceDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspace;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspaceInvite;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspaceMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceInviteRepository workspaceInviteRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private WorkspaceEventPublisher workspaceEventPublisher;

    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                workspaceInviteRepository,
                userServiceClient,
                workspaceEventPublisher
        );
    }

    @Test
    void createWorkspace_validRequest_persistsWorkspaceAndOwnerMembership() {
        UUID ownerAuthId = UUID.randomUUID();
        when(workspaceRepository.existsByPublicCode(any())).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workspace created = workspaceService.createWorkspace(createWorkspaceDTO(), ownerAuthId);

        assertThat(created.getOwnerAuthId()).isEqualTo(ownerAuthId);
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void inviteMemberByUsername_pendingInviteExists_throwsConflict() {
        UUID ownerAuthId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        Workspace workspace = workspace(ownerAuthId);

        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), ownerAuthId))
                .thenReturn(Optional.of(workspaceMember(workspace, ownerAuthId, WorkspaceRole.OWNER)));
        when(userServiceClient.resolveAuthIdByUsername("mary")).thenReturn(targetAuthId);
        when(workspaceInviteRepository.existsByWorkspaceIdAndTargetAuthIdAndStatus(workspace.getId(), targetAuthId, InviteStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> workspaceService.inviteMemberByUsername(workspace.getId(), addMemberByUsernameDTO("mary"), ownerAuthId))
                .isInstanceOf(MemberAlreadyExistsException.class);
    }

    @Test
    void acceptInvite_pendingInvite_createsMembershipAndMarksInviteAccepted() {
        UUID inviter = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        Workspace workspace = workspace(inviter);
        WorkspaceInvite invite = workspaceInvite(workspace, targetAuthId, inviter, InviteStatus.PENDING);

        when(workspaceInviteRepository.findByIdAndTargetAuthId(invite.getId(), targetAuthId)).thenReturn(Optional.of(invite));
        when(workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspace.getId(), targetAuthId)).thenReturn(false);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceMember member = workspaceService.acceptInvite(invite.getId(), targetAuthId);

        assertThat(member.getAuthId()).isEqualTo(targetAuthId);
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        verify(workspaceEventPublisher).publish("WORKSPACE_INVITE_ACCEPTED", inviter, targetAuthId, workspace, invite.getId().toString(), WorkspaceRole.MEMBER.name());
    }

    @Test
    void updateMemberRole_requesterIsNotOwner_throwsUnauthorizedAction() {
        UUID ownerAuthId = UUID.randomUUID();
        UUID adminAuthId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        Workspace workspace = workspace(ownerAuthId);

        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), adminAuthId))
                .thenReturn(Optional.of(workspaceMember(workspace, adminAuthId, WorkspaceRole.ADMIN)));

        assertThatThrownBy(() -> workspaceService.updateMemberRole(
                workspace.getId(),
                targetAuthId,
                com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.updateMemberRoleDTO(WorkspaceRole.ADMIN),
                adminAuthId
        )).isInstanceOf(UnauthorizedWorkspaceActionException.class);
    }

    @Test
    void removeMember_ownerTarget_throwsCannotRemoveOwner() {
        UUID ownerAuthId = UUID.randomUUID();
        Workspace workspace = workspace(ownerAuthId);

        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), ownerAuthId))
                .thenReturn(Optional.of(workspaceMember(workspace, ownerAuthId, WorkspaceRole.OWNER)));
        when(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), workspace.getOwnerAuthId()))
                .thenReturn(Optional.of(workspaceMember(workspace, workspace.getOwnerAuthId(), WorkspaceRole.OWNER)));

        assertThatThrownBy(() -> workspaceService.removeMember(workspace.getId(), workspace.getOwnerAuthId(), ownerAuthId))
                .isInstanceOf(CannotRemoveOwnerException.class);
    }

    @Test
    void listMembersWithProfiles_memberExists_enrichesWithProfileData() {
        UUID ownerAuthId = UUID.randomUUID();
        Workspace workspace = workspace(ownerAuthId);
        WorkspaceMember owner = workspaceMember(workspace, ownerAuthId, WorkspaceRole.OWNER);

        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspace.getId(), ownerAuthId)).thenReturn(true);
        when(workspaceMemberRepository.findAllByWorkspaceId(workspace.getId())).thenReturn(List.of(owner));
        when(userServiceClient.fetchProfilesBatch(List.of(ownerAuthId)))
                .thenReturn(Map.of(ownerAuthId, new UserServiceClient.UserProfileResponse(ownerAuthId, "john", "avatar.png")));

        assertThat(workspaceService.listMembersWithProfiles(workspace.getId(), ownerAuthId))
                .singleElement()
                .extracting("displayName")
                .isEqualTo("john");
    }
}
