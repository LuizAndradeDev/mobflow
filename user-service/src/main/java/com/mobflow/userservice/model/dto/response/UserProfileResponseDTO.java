package com.mobflow.userservice.model.dto.response;

import com.mobflow.userservice.model.entities.UserProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponseDTO {

    private UUID id;
    private UUID authId;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileResponseDTO fromEntity(UserProfile profile) {
        return UserProfileResponseDTO.builder()
                .id(profile.getId())
                .authId(profile.getAuthId())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .phone(profile.getPhone())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
