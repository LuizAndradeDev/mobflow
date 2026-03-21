package com.mobflow.workspaceservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberByUsernameDTO {

    @NotBlank(message = "Username is required")
    private String username;
}
