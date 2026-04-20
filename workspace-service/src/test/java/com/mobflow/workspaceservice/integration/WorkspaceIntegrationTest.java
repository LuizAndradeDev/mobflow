package com.mobflow.workspaceservice.integration;

import com.mobflow.workspaceservice.config.UserServiceClient;
import com.mobflow.workspaceservice.events.WorkspaceEventPublisher;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.InviteStatus;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.repository.WorkspaceInviteRepository;
import com.mobflow.workspaceservice.repository.WorkspaceMemberRepository;
import com.mobflow.workspaceservice.repository.WorkspaceRepository;
import com.mobflow.workspaceservice.service.WorkspaceService;
import com.mobflow.workspaceservice.testsupport.AbstractPostgresWorkspaceServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;
import java.util.UUID;

import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.addMemberByUsernameDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.createWorkspaceDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "security.jwt.secret-key=c3VwZXItc2VjdXJlLXRlc3Qta2V5LWZvci13b3Jrc3BhY2Utc2VydmljZS0xMjM0NTY3ODkw",
        "user.service.url=http://localhost:8081",
        "internal.secret=test-internal-secret"
})
class WorkspaceIntegrationTest extends AbstractPostgresWorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceInviteRepository workspaceInviteRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private WorkspaceEventPublisher workspaceEventPublisher;

    @BeforeEach
    void cleanUp() {
        workspaceInviteRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void createWorkspace_validRequest_persistsWorkspaceAndOwnerMember() {
        UUID ownerAuthId = UUID.randomUUID();

        Workspace workspace = workspaceService.createWorkspace(createWorkspaceDTO(), ownerAuthId);

        assertThat(workspaceRepository.findById(workspace.getId())).isPresent();
        assertThat(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), ownerAuthId)).isPresent();
    }

    @Test
    void inviteAndAcceptInvite_validFlow_persistsInviteAndMembership() {
        UUID ownerAuthId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        Workspace workspace = workspaceService.createWorkspace(createWorkspaceDTO(), ownerAuthId);

        when(userServiceClient.resolveAuthIdByUsername("mary")).thenReturn(targetAuthId);
        when(userServiceClient.fetchProfilesBatch(anyList()))
                .thenReturn(Map.of(
                        ownerAuthId, new UserServiceClient.UserProfileResponse(ownerAuthId, "john", null),
                        targetAuthId, new UserServiceClient.UserProfileResponse(targetAuthId, "mary", null)
                ));

        WorkspaceInvite invite = workspaceService.inviteMemberByUsername(workspace.getId(), addMemberByUsernameDTO("mary"), ownerAuthId);
        WorkspaceMember member = workspaceService.acceptInvite(invite.getId(), targetAuthId);

        assertThat(workspaceInviteRepository.findById(invite.getId())).isPresent().get()
                .extracting(WorkspaceInvite::getStatus)
                .isEqualTo(InviteStatus.ACCEPTED);
        assertThat(member.getRole()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspace.getId(), targetAuthId)).isPresent();
        verify(workspaceEventPublisher).publish(
                "WORKSPACE_INVITE",
                targetAuthId,
                ownerAuthId,
                targetAuthId,
                workspace,
                invite.getId().toString(),
                null
        );
    }
}
