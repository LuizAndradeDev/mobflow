package com.mobflow.chatservice.model.dto.response;

import com.mobflow.chatservice.model.entities.ConversationLastMessage;
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
public class ConversationLastMessageResponseDTO {

    private UUID messageId;
    private UUID senderId;
    private String contentPreview;
    private MessageContentType contentType;
    private Instant createdAt;

    public static ConversationLastMessageResponseDTO fromEntity(ConversationLastMessage lastMessage) {
        if (lastMessage == null) {
            return null;
        }

        return ConversationLastMessageResponseDTO.builder()
                .messageId(lastMessage.getMessageId())
                .senderId(lastMessage.getSenderId())
                .contentPreview(lastMessage.getContentPreview())
                .contentType(lastMessage.getContentType())
                .createdAt(lastMessage.getCreatedAt())
                .build();
    }
}
