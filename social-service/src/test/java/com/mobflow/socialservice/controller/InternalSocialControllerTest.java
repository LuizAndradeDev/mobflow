package com.mobflow.socialservice.controller;

import com.mobflow.socialservice.config.SecurityConfig;
import com.mobflow.socialservice.exception.GlobalExceptionHandler;
import com.mobflow.socialservice.security.JwtAuthenticationFilter;
import com.mobflow.socialservice.service.FriendshipService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalSocialController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "internal.secret=test-internal-secret")
class InternalSocialControllerTest {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendshipService friendshipService;

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
    void validateFriendship_validSecretAndExistingFriendship_returnsOk() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        when(friendshipService.areFriends(eq(authId), eq(targetAuthId))).thenReturn(true);

        mockMvc.perform(get("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", authId, targetAuthId)
                        .contextPath("/social")
                        .header(INTERNAL_SECRET_HEADER, "test-internal-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void validateFriendship_validSecretAndMissingFriendship_returnsNotFound() throws Exception {
        UUID authId = UUID.randomUUID();
        UUID targetAuthId = UUID.randomUUID();
        when(friendshipService.areFriends(eq(authId), eq(targetAuthId))).thenReturn(false);

        mockMvc.perform(get("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", authId, targetAuthId)
                        .contextPath("/social")
                        .header(INTERNAL_SECRET_HEADER, "test-internal-secret"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validateFriendship_invalidSecret_returnsForbidden() throws Exception {
        mockMvc.perform(get("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", UUID.randomUUID(), UUID.randomUUID())
                        .contextPath("/social")
                        .header(INTERNAL_SECRET_HEADER, "wrong-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateFriendship_missingSecret_returnsForbidden() throws Exception {
        mockMvc.perform(get("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", UUID.randomUUID(), UUID.randomUUID())
                        .contextPath("/social"))
                .andExpect(status().isForbidden());
    }
}
