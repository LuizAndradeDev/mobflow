package com.mobflow.userservice.testsupport;

import com.mobflow.userservice.model.dto.request.UpdateUserProfileDTO;
import com.mobflow.userservice.model.entities.UserProfile;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

public final class UserServiceTestFixtures {

    private UserServiceTestFixtures() {
    }

    public static UserProfile userProfile() {
        return userProfile(UUID.randomUUID(), "john");
    }

    public static UserProfile userProfile(UUID authId, String displayName) {
        return UserProfile.builder()
                .id(UUID.randomUUID())
                .authId(authId)
                .displayName(displayName)
                .bio("Bio")
                .avatarUrl("http://cdn.mobflow.dev/avatar.png")
                .phone("555-0101")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static UpdateUserProfileDTO updateUserProfileDTO(String displayName, String bio, String phone) {
        UpdateUserProfileDTO dto = new UpdateUserProfileDTO();
        dto.setDisplayName(displayName);
        dto.setBio(bio);
        dto.setPhone(phone);
        return dto;
    }

    public static MockMultipartFile avatarFile() {
        return new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "avatar".getBytes(StandardCharsets.UTF_8)
        );
    }
}
