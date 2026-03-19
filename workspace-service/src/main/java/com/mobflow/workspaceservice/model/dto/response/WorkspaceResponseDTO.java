package com.mobflow.workspaceservice.model.dto.response;

import com.mobflow.workspaceservice.model.entities.Workspace;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkspaceResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerAuthId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkspaceResponseDTO fromEntity(Workspace workspace) {
        return WorkspaceResponseDTO.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerAuthId(workspace.getOwnerAuthId())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}
