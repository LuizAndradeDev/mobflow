package com.mobflow.workspaceservice.model.dto.request;

import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleDTO {

    @NotNull(message = "Role is required")
    private WorkspaceRole role;
}
