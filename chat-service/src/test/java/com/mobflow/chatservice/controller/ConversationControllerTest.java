package com.mobflow.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.chatservice.config.SecurityConfig;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.exception.GlobalExceptionHandler;
import com.mobflow.chatservice.model.dto.response.ConversationResponseDTO;
import com.mobflow.chatservice.model.dto.response.ConversationUpsertResponseDTO;
import com.mobflow.chatservice.security.JwtAuthenticationFilter;
import com.mobflow.chatservice.service.ConversationService;
import com.mobflow.chatservice.service.MessageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mobflow.chatservice.testsupport.JwtTestHelper.authentication;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private MessageService messageService;

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
    void createOrGetPrivateConversation_authenticatedRequest_returnsCreatedResponse() throws Exception {
        when(conversationService.createOrGetPrivateConversation(any(), any())).thenReturn(
                ConversationUpsertResponseDTO.builder()
                        .created(true)
                        .conversation(conversationResponse())
                        .build()
        );

        mockMvc.perform(post("/chat/api/conversations/private")
                        .contextPath("/chat")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("targetAuthId", UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.conversation.counterpartAuthId").exists());
    }

    @Test
    void listConversations_authenticatedRequest_returnsConversationArray() throws Exception {
        when(conversationService.listConversations(any())).thenReturn(List.of(conversationResponse()));

        mockMvc.perform(get("/chat/api/conversations")
                        .contextPath("/chat")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PRIVATE"));
    }

    @Test
    void getConversation_forbiddenOperation_returnsForbiddenErrorResponse() throws Exception {
        when(conversationService.getConversation(any(), any())).thenThrow(ChatServiceException.accessDenied());

        mockMvc.perform(get("/chat/api/conversations/{conversationId}", UUID.randomUUID())
                        .contextPath("/chat")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void createOrGetPrivateConversation_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/chat/api/conversations/private")
                        .contextPath("/chat")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void listConversations_unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/chat/api/conversations").contextPath("/chat"))
                .andExpect(status().isUnauthorized());
    }

    private ConversationResponseDTO conversationResponse() {
        return ConversationResponseDTO.builder()
                .id(UUID.randomUUID())
                .type(com.mobflow.chatservice.model.enums.ConversationType.PRIVATE)
                .participantIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .counterpartAuthId(UUID.randomUUID())
                .lastActivityAt(Instant.now())
                .unreadCount(2L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
