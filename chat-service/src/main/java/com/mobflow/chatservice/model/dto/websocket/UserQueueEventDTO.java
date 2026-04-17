package com.mobflow.chatservice.model.dto.websocket;

import com.mobflow.chatservice.model.dto.response.ConversationResponseDTO;
import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.model.enums.ChatEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQueueEventDTO {

    private ChatEventType eventType;
    private ConversationResponseDTO conversation;
    private MessageResponseDTO message;
    private Instant occurredAt;
}
