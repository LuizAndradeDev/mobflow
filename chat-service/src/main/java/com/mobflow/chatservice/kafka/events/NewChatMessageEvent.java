package com.mobflow.chatservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewChatMessageEvent {

    private String eventType;
    private String messageId;
    private String conversationId;
    private String senderId;
    private String recipientId;
    private String contentPreview;
    private Instant createdAt;
}
