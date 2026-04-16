package com.mobflow.socialservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank(message = "content is required")
    @Size(max = 4000, message = "content must have at most 4000 characters")
    private String content;
}
