package com.mobflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.config.SecurityConfiguration;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.security.JwtAuthenticationFilter;
import com.mobflow.taskservice.service.TaskService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createTaskRequest;
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

@WebMvcTest(TaskController.class)
@Import(SecurityConfiguration.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

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
    void createTask_authenticatedRequest_returnsCreatedTask() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        when(taskService.createTask(eq(workspaceId), eq(listId), eq(authId), any()))
                .thenReturn(taskResponse(taskId, listId, workspaceId, assigneeAuthId));

        mockMvc.perform(post("/api/workspaces/{workspaceId}/lists/{listId}/tasks", workspaceId, listId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest(assigneeAuthId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.assigneeAuthId").value(assigneeAuthId.toString()));
    }

    @Test
    void createTask_invalidPayload_returnsBadRequest() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        mockMvc.perform(post("/api/workspaces/{workspaceId}/lists/{listId}/tasks", workspaceId, listId)
                        .with(authentication(auth(UUID.randomUUID())))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTasks_authenticatedRequest_returnsTaskArray() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        when(taskService.listTasksByList(listId)).thenReturn(List.of(taskResponse(UUID.randomUUID(), listId, workspaceId, null)));

        mockMvc.perform(get("/api/workspaces/{workspaceId}/lists/{listId}/tasks", workspaceId, listId)
                        .with(authentication(auth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listId").value(listId.toString()));
    }

    @Test
    void updateTask_authenticatedRequest_returnsUpdatedTask() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Renamed");

        when(taskService.updateTask(eq(workspaceId), eq(taskId), eq(authId), any()))
                .thenReturn(taskResponse(taskId, UUID.randomUUID(), workspaceId, null));

        mockMvc.perform(put("/api/workspaces/{workspaceId}/tasks/{taskId}", workspaceId, taskId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void moveTask_authenticatedRequest_returnsMovedTask() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        MoveTaskRequest request = new MoveTaskRequest();
        request.setTargetListId(listId);
        request.setPosition(1);

        when(taskService.moveTask(eq(workspaceId), eq(taskId), eq(authId), any()))
                .thenReturn(taskResponse(taskId, listId, workspaceId, null));

        mockMvc.perform(patch("/api/workspaces/{workspaceId}/tasks/{taskId}/move", workspaceId, taskId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listId").value(listId.toString()));
    }

    @Test
    void deleteTask_unauthenticatedRequest_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/workspaces/{workspaceId}/tasks/{taskId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    private TaskResponseDTO taskResponse(UUID taskId, UUID listId, UUID workspaceId, UUID assigneeAuthId) {
        return TaskResponseDTO.builder()
                .id(taskId)
                .listId(listId)
                .workspaceId(workspaceId)
                .title("Prepare release")
                .assigneeAuthId(assigneeAuthId)
                .dueDate(LocalDate.now().plusDays(2))
                .status(com.mobflow.taskservice.model.enums.TaskStatus.TODO)
                .build();
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken(
                "john",
                authId,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
