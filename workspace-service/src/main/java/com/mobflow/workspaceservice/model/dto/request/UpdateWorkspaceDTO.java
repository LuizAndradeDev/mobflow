package com.mobflow.workspaceservice.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWorkspaceDTO {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
