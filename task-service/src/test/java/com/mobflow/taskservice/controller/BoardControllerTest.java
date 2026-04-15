package com.mobflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.config.SecurityConfiguration;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.security.JwtAuthenticationFilter;
import com.mobflow.taskservice.service.BoardService;
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

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createBoardRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardController.class)
@Import(SecurityConfiguration.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

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
    void listBoards_authenticatedRequest_returnsBoardArray() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        when(boardService.listBoards(workspaceId)).thenReturn(List.of(BoardResponseDTO.builder()
                .id(boardId)
                .workspaceId(workspaceId)
                .name("Platform")
                .build()));

        mockMvc.perform(get("/api/workspaces/{workspaceId}/boards", workspaceId)
                        .with(authentication(auth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(boardId.toString()));
    }

    @Test
    void createBoard_authenticatedRequest_returnsCreatedBoard() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        when(boardService.createBoard(eq(workspaceId), eq(authId), any())).thenReturn(BoardResponseDTO.builder()
                .id(boardId)
                .workspaceId(workspaceId)
                .name("Platform")
                .color("#111827")
                .build());

        mockMvc.perform(post("/api/workspaces/{workspaceId}/boards", workspaceId)
                        .with(authentication(auth(authId)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBoardRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Platform"));
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken("john", authId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
