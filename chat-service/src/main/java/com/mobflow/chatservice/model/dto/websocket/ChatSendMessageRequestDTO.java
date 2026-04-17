package com.mobflow.chatservice.model.dto.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendMessageRequestDTO {

    @NotNull(message = "conversationId is required")
    private UUID conversationId;

    @NotBlank(message = "content must not be blank")
    private String content;
}
