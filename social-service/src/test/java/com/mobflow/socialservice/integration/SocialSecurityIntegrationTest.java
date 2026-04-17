package com.mobflow.socialservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.client.WorkspaceServiceClient;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.model.enums.WorkspaceRole;
import com.mobflow.socialservice.repository.CommentRepository;
import com.mobflow.socialservice.repository.FriendRequestRepository;
import com.mobflow.socialservice.testsupport.AbstractSocialIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.WORKSPACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocialSecurityIntegrationTest extends AbstractSocialIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @MockBean
    private TaskServiceClient taskServiceClient;

    @MockBean
    private WorkspaceServiceClient workspaceServiceClient;

    @MockBean
    private AuthServiceClient authServiceClient;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        friendRequestRepository.deleteAll();
        when(taskServiceClient.getTaskContext(TASK_ID))
                .thenReturn(new TaskServiceClient.TaskCommentContextResponse(
                        TASK_ID,
                        WORKSPACE_ID,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Prepare roadmap"
                ));
        when(workspaceServiceClient.requireMembership(eq(WORKSPACE_ID), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(WorkspaceRole.MEMBER);
        when(workspaceServiceClient.isWorkspaceMember(eq(WORKSPACE_ID), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(true);
        when(authServiceClient.resolveByUsernames(anyList())).thenReturn(java.util.Map.of());
    }

    @Test
    void createComment_missingJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(withSocialContextPath(post("/social/api/tasks/{taskId}/comments", TASK_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello team"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createComment_invalidJwt_returnsForbidden() throws Exception {
        mockMvc.perform(withSocialContextPath(post("/social/api/tasks/{taskId}/comments", TASK_ID))
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello team"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Authentication Error"));
    }

    @Test
    void createComment_validJwt_usesIdentityFromToken() throws Exception {
        UUID authId = UUID.randomUUID();

        mockMvc.perform(withSocialContextPath(post("/social/api/tasks/{taskId}/comments", TASK_ID))
                        .header("Authorization", bearerToken(authId, "john_dev"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello team"))))
                .andExpect(status().isCreated());

        Comment storedComment = commentRepository.findAll().getFirst();
        assertThat(storedComment.getAuthorId()).isEqualTo(authId);
        assertThat(storedComment.getAuthorUsername()).isEqualTo("john_dev");
    }

    @Test
    void updateComment_differentAuthor_returnsForbidden() throws Exception {
        Comment comment = Comment.create(TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "owner_user", "Original", List.of());
        comment.setId(UUID.randomUUID());
        commentRepository.save(comment);

        mockMvc.perform(withSocialContextPath(put("/social/api/comments/{commentId}", comment.getId()))
                        .header("Authorization", bearerToken(UUID.randomUUID(), "other_user"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Updated"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ACCESS_DENIED"));
    }

    @Test
    void acceptFriendRequest_wrongTargetUser_returnsForbidden() throws Exception {
        FriendRequest friendRequest = FriendRequest.create(UUID.randomUUID(), "john_dev", UUID.randomUUID(), "mary_dev");
        friendRequest.setId(UUID.randomUUID());
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequestRepository.save(friendRequest);

        mockMvc.perform(withSocialContextPath(post("/social/api/friends/{requestId}/accept", friendRequest.getId()))
                        .header("Authorization", bearerToken(UUID.randomUUID(), "other_user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ACCESS_DENIED"));
    }
}
