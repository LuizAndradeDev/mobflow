package com.mobflow.socialservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.config.SecurityConfig;
import com.mobflow.socialservice.exception.GlobalExceptionHandler;
import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.dto.response.CommentResponse;
import com.mobflow.socialservice.security.JwtAuthenticationFilter;
import com.mobflow.socialservice.service.CommentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.JwtTestHelper.authentication;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

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
    void createComment_authenticatedRequest_returnsCreatedResponse() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        when(commentService.createComment(any(), any(), any())).thenReturn(commentResponse(commentId, taskId, false));

        mockMvc.perform(post("/social/api/tasks/{taskId}/comments", taskId)
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello team"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void listComments_authenticatedRequest_returnsPagedContent() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(commentService.listComments(any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PageImpl<>(List.of(commentResponse(UUID.randomUUID(), taskId, false))));

        mockMvc.perform(get("/social/api/tasks/{taskId}/comments", taskId)
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].taskId").value(taskId.toString()));
    }

    @Test
    void updateComment_authenticatedRequest_returnsUpdatedComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        when(commentService.updateComment(any(), any(), any())).thenReturn(commentResponse(commentId, UUID.randomUUID(), false));

        mockMvc.perform(put("/social/api/comments/{commentId}", commentId)
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()));
    }

    @Test
    void deleteComment_authenticatedRequest_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/social/api/comments/{commentId}", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void createComment_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/social/api/tasks/{taskId}/comments", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void createComment_unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/social/api/tasks/{taskId}/comments", UUID.randomUUID())
                        .contextPath("/social")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello team"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateComment_forbiddenOperation_returnsForbiddenProblemDetail() throws Exception {
        when(commentService.updateComment(any(), any(), any())).thenThrow(SocialServiceException.accessDenied());

        mockMvc.perform(put("/social/api/comments/{commentId}", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Updated"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ACCESS_DENIED"));
    }

    @Test
    void deleteComment_commentNotFound_returnsNotFoundProblemDetail() throws Exception {
        org.mockito.Mockito.doThrow(SocialServiceException.commentNotFound())
                .when(commentService)
                .deleteComment(any(), any());

        mockMvc.perform(delete("/social/api/comments/{commentId}", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("COMMENT_NOT_FOUND"));
    }

    private CommentResponse commentResponse(UUID commentId, UUID taskId, boolean deleted) {
        return CommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .workspaceId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .authorUsername("john_dev")
                .content(deleted ? null : "Hello team")
                .mentions(List.of("mary_dev"))
                .createdAt(Instant.now())
                .editedAt(null)
                .deleted(deleted)
                .build();
    }
}
