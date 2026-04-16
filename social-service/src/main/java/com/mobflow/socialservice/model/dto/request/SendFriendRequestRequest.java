package com.mobflow.socialservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendFriendRequestRequest {

    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must have at most 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "username contains invalid characters")
    private String username;
}
