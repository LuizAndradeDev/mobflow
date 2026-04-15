package com.mobflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.config.SecurityConfiguration;
import com.mobflow.taskservice.security.JwtAuthenticationFilter;
import com.mobflow.taskservice.service.AnalyticsService;
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

import static com.mobflow.taskservice.model.dto.response.TaskAnalyticsResponseDTO.fromValues;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfiguration.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

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
    void getWorkspaceAnalytics_matchingAuthenticatedUser_returnsAnalytics() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        when(analyticsService.getWorkspaceAnalytics(workspaceId, authId)).thenReturn(fromValues(10, 4, 5, 6, 1));

        mockMvc.perform(get("/api/task-analytics/workspace/{workspaceId}/user/{authId}", workspaceId, authId)
                        .with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(4));
    }

    @Test
    void getUserAnalyticsAcrossWorkspaces_mismatchedAuthenticatedUser_returnsForbidden() throws Exception {
        UUID authId = UUID.randomUUID();

        mockMvc.perform(post("/api/task-analytics/user/{authId}/workspaces", authId)
                        .with(authentication(auth(UUID.randomUUID())))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken("john", authId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
