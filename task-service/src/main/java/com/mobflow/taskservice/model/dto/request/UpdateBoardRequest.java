package com.mobflow.taskservice.model.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBoardRequest {

    @Size(min = 2, max = 100, message = "Board name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g. #6366f1)")
    private String color;
}
