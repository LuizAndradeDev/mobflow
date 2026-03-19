package com.mobflow.workspaceservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddMemberDTO {

    @NotNull(message = "Auth ID is required")
    private UUID authId;
}
