package com.mobflow.chatservice.model.dto.websocket;

import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.model.enums.ChatEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTopicEventDTO {

    private ChatEventType eventType;
    private UUID conversationId;
    private MessageResponseDTO message;
    private ConversationReadReceiptDTO readReceipt;
    private Instant occurredAt;
}
