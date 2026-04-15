package com.mobflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.config.SecurityConfiguration;
import com.mobflow.taskservice.model.dto.request.ReorderListsRequest;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.security.JwtAuthenticationFilter;
import com.mobflow.taskservice.service.TaskListService;
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

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createTaskListRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskListController.class)
@Import(SecurityConfiguration.class)
class TaskListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskListService taskListService;

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
    void createList_authenticatedRequest_returnsCreatedList() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        when(taskListService.createList(eq(workspaceId), eq(boardId), eq(authId), any())).thenReturn(TaskListResponseDTO.builder()
                .id(listId)
                .boardId(boardId)
                .name("Doing")
                .build());

        mockMvc.perform(post("/api/workspaces/{workspaceId}/boards/{boardId}/lists", workspaceId, boardId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskListRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Doing"));
    }

    @Test
    void reorderLists_authenticatedRequest_returnsNoContent() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        ReorderListsRequest request = new ReorderListsRequest();
        request.setOrderedIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        doNothing().when(taskListService).reorderLists(eq(workspaceId), eq(boardId), eq(authId), any());

        mockMvc.perform(patch("/api/workspaces/{workspaceId}/boards/{boardId}/lists/reorder", workspaceId, boardId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken("john", authId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
