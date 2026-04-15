package com.mobflow.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.userservice.config.SecurityConfiguration;
import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.security.JwtAuthenticationFilter;
import com.mobflow.userservice.services.UserProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.avatarFile;
import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.updateUserProfileDTO;
import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.userProfile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfiguration.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

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
    void getMyProfile_authenticatedUser_returnsProfile() throws Exception {
        UUID authId = UUID.randomUUID();
        UserProfile profile = userProfile(authId, "john");
        when(userProfileService.findOrCreateProfile(authId, "john")).thenReturn(profile);

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(auth(authId, "john"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authId").value(authId.toString()))
                .andExpect(jsonPath("$.displayName").value("john"));
    }

    @Test
    void getMyProfile_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyProfile_validPayload_returnsUpdatedProfile() throws Exception {
        UUID authId = UUID.randomUUID();
        UserProfile updated = userProfile(authId, "johnny");
        when(userProfileService.updateProfile(eq(authId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/users/me")
                        .with(authentication(auth(authId, "john")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserProfileDTO("johnny", "Bio", "99999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("johnny"));
    }

    @Test
    void updateMyProfile_invalidPayload_returnsBadRequest() throws Exception {
        UUID authId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/me")
                        .with(authentication(auth(authId, "john")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserProfileDTO("a", "Bio", "99999"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAvatar_validFile_returnsUpdatedProfile() throws Exception {
        UUID authId = UUID.randomUUID();
        UserProfile updated = userProfile(authId, "john");
        when(userProfileService.updateAvatar(eq(authId), any())).thenReturn(updated);

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(avatarFile())
                        .with(authentication(auth(authId, "john")))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("john"));
    }

    @Test
    void getProfileByAuthId_authenticated_returnsProfile() throws Exception {
        UUID authId = UUID.randomUUID();
        UserProfile profile = userProfile(authId, "john");
        when(userProfileService.getProfileByAuthId(authId)).thenReturn(profile);

        mockMvc.perform(get("/api/users/{authId}", authId)
                        .with(authentication(auth(UUID.randomUUID(), "viewer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authId").value(authId.toString()));
    }

    private UsernamePasswordAuthenticationToken auth(UUID authId, String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                authId,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
