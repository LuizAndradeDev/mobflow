package com.mobflow.workspaceservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.workspaceservice.config.SecurityConfiguration;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceInvite;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.InviteStatus;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.security.JwtAuthenticationFilter;
import com.mobflow.workspaceservice.service.WorkspaceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.addMemberByUsernameDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.createWorkspaceDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.updateMemberRoleDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.updateWorkspaceDTO;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspace;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspaceInvite;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspaceMember;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
@Import(SecurityConfiguration.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(
                    invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class)
            );
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void create_authenticatedRequest_returnsCreatedWorkspace() throws Exception {
        UUID authId = UUID.randomUUID();
        Workspace workspace = workspace(authId);
        when(workspaceService.createWorkspace(any(), eq(authId))).thenReturn(workspace);

        mockMvc.perform(post("/api/workspaces")
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWorkspaceDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workspace.getId().toString()));
    }

    @Test
    void listMine_authenticatedRequest_returnsWorkspaceList() throws Exception {
        UUID authId = UUID.randomUUID();
        when(workspaceService.listMyWorkspaces(authId)).thenReturn(List.of(workspace(authId)));

        mockMvc.perform(get("/api/workspaces").with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerAuthId").value(authId.toString()));
    }

    @Test
    void previewByCode_publicRequest_returnsWorkspace() throws Exception {
        Workspace workspace = workspace(UUID.randomUUID());
        when(workspaceService.getWorkspaceByCode("ABC12345")).thenReturn(workspace);

        mockMvc.perform(get("/api/workspaces/join/ABC12345").with(authentication(auth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicCode").value("ABC12345"));
    }

    @Test
    void inviteMember_authenticatedRequest_returnsCreatedInvite() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Workspace workspace = workspace(authId);
        WorkspaceInvite invite = workspaceInvite(workspace, target, authId, InviteStatus.PENDING);
        when(workspaceService.inviteMemberByUsername(eq(workspace.getId()), any(), eq(authId))).thenReturn(invite);

        mockMvc.perform(post("/api/workspaces/{id}/invites", workspace.getId())
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberByUsernameDTO("mary"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetAuthId").value(target.toString()));
    }

    @Test
    void acceptInvite_authenticatedRequest_returnsMembership() throws Exception {
        UUID authId = UUID.randomUUID();
        Workspace workspace = workspace(UUID.randomUUID());
        WorkspaceMember member = workspaceMember(workspace, authId, WorkspaceRole.MEMBER);
        when(workspaceService.acceptInvite(UUID.fromString("11111111-1111-1111-1111-111111111111"), authId)).thenReturn(member);

        mockMvc.perform(post("/api/workspaces/invites/{inviteId}/accept", "11111111-1111-1111-1111-111111111111")
                        .with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authId").value(authId.toString()));
    }

    @Test
    void updateMemberRole_authenticatedRequest_returnsUpdatedRole() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Workspace workspace = workspace(authId);
        WorkspaceMember member = workspaceMember(workspace, target, WorkspaceRole.ADMIN);
        when(workspaceService.updateMemberRole(eq(workspace.getId()), eq(target), any(), eq(authId))).thenReturn(member);

        mockMvc.perform(patch("/api/workspaces/{id}/members/{memberAuthId}/role", workspace.getId(), target)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMemberRoleDTO(WorkspaceRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void create_unauthenticatedRequest_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWorkspaceDTO())))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken(
                "john",
                authId,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
