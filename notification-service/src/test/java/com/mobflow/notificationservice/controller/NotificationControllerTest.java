package com.mobflow.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.notificationservice.config.SecurityConfig;
import com.mobflow.notificationservice.model.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;
import com.mobflow.notificationservice.security.JwtAuthenticationFilter;
import com.mobflow.notificationservice.service.NotificationService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

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
    void list_authenticatedRequest_returnsNotificationsForTokenRecipient() throws Exception {
        UUID authId = UUID.randomUUID();
        when(notificationService.listForUser(authId.toString())).thenReturn(List.of(notificationResponse(authId.toString())));

        mockMvc.perform(get("/api/notifications").with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientId").value(authId.toString()))
                .andExpect(jsonPath("$[0].type").value("TASK_ASSIGNED"));
    }

    @Test
    void unreadCount_authenticatedRequest_returnsUnreadCountPayload() throws Exception {
        UUID authId = UUID.randomUUID();
        when(notificationService.countUnread(authId.toString())).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count").with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void markAsRead_authenticatedRequest_returnsUpdatedNotification() throws Exception {
        UUID authId = UUID.randomUUID();
        when(notificationService.markAsRead(eq("notification-1"), eq(authId.toString()))).thenReturn(notificationResponse(authId.toString()));

        mockMvc.perform(patch("/api/notifications/{id}/read", "notification-1").with(authentication(auth(authId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("notification-1"));
    }

    @Test
    void list_unauthenticatedRequest_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    private NotificationResponseDTO notificationResponse(String recipientId) {
        return new NotificationResponseDTO(
                "notification-1",
                recipientId,
                "user@mobflow.test",
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.IN_APP,
                NotificationPriority.MEDIUM,
                "Task assigned",
                "A task was assigned",
                false,
                Instant.now(),
                null,
                null,
                null,
                Map.of("taskId", "task-1")
        );
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId) {
        return new UsernamePasswordAuthenticationToken("john", authId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
