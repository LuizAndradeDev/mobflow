package com.mobflow.socialservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.config.SecurityConfig;
import com.mobflow.socialservice.exception.GlobalExceptionHandler;
import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.dto.response.FriendRequestResponse;
import com.mobflow.socialservice.model.dto.response.FriendResponse;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void sendFriendRequest_authenticatedRequest_returnsCreatedResponse() throws Exception {
        when(friendshipService.sendFriendRequest(any(), any())).thenReturn(friendRequestResponse(FriendRequestStatus.PENDING));

        mockMvc.perform(post("/social/api/friends/request")
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", "mary_dev"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listFriendRequests_authenticatedRequest_returnsRequestArray() throws Exception {
        when(friendshipService.listFriendRequests(any())).thenReturn(List.of(friendRequestResponse(FriendRequestStatus.PENDING)));

        mockMvc.perform(get("/social/api/friends/requests")
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetUsername").value("mary_dev"));
    }

    @Test
    void acceptFriendRequest_authenticatedRequest_returnsAcceptedResponse() throws Exception {
        when(friendshipService.acceptFriendRequest(any(), any())).thenReturn(friendRequestResponse(FriendRequestStatus.ACCEPTED));

        mockMvc.perform(post("/social/api/friends/{requestId}/accept", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "mary_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void declineFriendRequest_authenticatedRequest_returnsOkResponse() throws Exception {
        when(friendshipService.declineFriendRequest(any(), any())).thenReturn(friendRequestResponse(FriendRequestStatus.DECLINED));

        mockMvc.perform(post("/social/api/friends/{requestId}/decline", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "mary_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void listFriends_authenticatedRequest_returnsFriendArray() throws Exception {
        when(friendshipService.listFriends(any())).thenReturn(List.of(
                FriendResponse.of(UUID.randomUUID(), "mary_dev", "http://cdn.mobflow.dev/mary.png", Instant.now())
        ));

        mockMvc.perform(get("/social/api/friends")
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("mary_dev"))
                .andExpect(jsonPath("$[0].avatarUrl").value("http://cdn.mobflow.dev/mary.png"));
    }

    @Test
    void sendFriendRequest_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/social/api/friends/request")
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void listFriends_unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/social/api/friends").contextPath("/social"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptFriendRequest_forbiddenOperation_returnsForbidden() throws Exception {
        when(friendshipService.acceptFriendRequest(any(), any())).thenThrow(SocialServiceException.accessDenied());

        mockMvc.perform(post("/social/api/friends/{requestId}/accept", UUID.randomUUID())
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ACCESS_DENIED"));
    }

    @Test
    void sendFriendRequest_duplicatePendingRequest_returnsConflict() throws Exception {
        when(friendshipService.sendFriendRequest(any(), any())).thenThrow(SocialServiceException.friendRequestAlreadyExists());

        mockMvc.perform(post("/social/api/friends/request")
                        .contextPath("/social")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication(UUID.randomUUID(), "john_dev")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", "mary_dev"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("FRIEND_REQUEST_ALREADY_EXISTS"));
    }

    private FriendRequestResponse friendRequestResponse(FriendRequestStatus status) {
        return FriendRequestResponse.builder()
                .id(UUID.randomUUID())
                .requesterId(UUID.randomUUID())
                .requesterUsername("john_dev")
                .targetId(UUID.randomUUID())
                .targetUsername("mary_dev")
                .status(status)
                .createdAt(Instant.now())
                .respondedAt(status == FriendRequestStatus.PENDING ? null : Instant.now())
                .incoming(false)
                .build();
    }
}
