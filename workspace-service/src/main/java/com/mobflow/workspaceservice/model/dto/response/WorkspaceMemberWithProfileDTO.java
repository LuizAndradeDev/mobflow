package com.mobflow.workspaceservice.model.dto.response;

import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkspaceMemberWithProfileDTO {

    private UUID id;
    private UUID workspaceId;
    private UUID authId;
    private WorkspaceRole role;
    private LocalDateTime joinedAt;
    private String displayName;
    private String avatarUrl;

    public static WorkspaceMemberWithProfileDTO fromMember(WorkspaceMember member) {
        return WorkspaceMemberWithProfileDTO.builder()
                .id(member.getId())
                .workspaceId(member.getWorkspace().getId())
                .authId(member.getAuthId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .displayName(member.getAuthId().toString().substring(0, 8))
                .avatarUrl(null)
                .build();
    }

    public static WorkspaceMemberWithProfileDTO fromMemberWithProfile(
            WorkspaceMember member,
            String displayName,
            String avatarUrl
    ) {
        return WorkspaceMemberWithProfileDTO.builder()
                .id(member.getId())
                .workspaceId(member.getWorkspace().getId())
                .authId(member.getAuthId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .displayName(displayName != null ? displayName : member.getAuthId().toString().substring(0, 8))
                .avatarUrl(avatarUrl)
                .build();
    }
}
