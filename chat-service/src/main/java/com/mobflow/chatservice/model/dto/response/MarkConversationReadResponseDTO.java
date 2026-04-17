package com.mobflow.chatservice.model.dto.response;

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
public class MarkConversationReadResponseDTO {

    private UUID conversationId;
    private long markedCount;
    private long unreadCount;
    private Instant readAt;
}
