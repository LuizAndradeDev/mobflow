package com.mobflow.chatservice.model.dto.websocket;

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
public class ConversationReadReceiptDTO {

    private UUID conversationId;
    private UUID readerAuthId;
    private long readCount;
    private Instant readAt;
}
