package com.mobflow.chatservice.model.entities;

import com.mobflow.chatservice.model.enums.MessageContentType;
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
public class ConversationLastMessage {

    private UUID messageId;
    private UUID senderId;
    private String contentPreview;
    private MessageContentType contentType;
    private Instant createdAt;

    public static ConversationLastMessage fromMessage(Message message) {
        return ConversationLastMessage.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .contentPreview(message.getContent())
                .contentType(message.getContentType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
