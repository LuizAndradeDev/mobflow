package com.mobflow.userservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BatchUserResponseDTO {
    private UUID authId;
    private String displayName;
    private String avatarUrl;
}
