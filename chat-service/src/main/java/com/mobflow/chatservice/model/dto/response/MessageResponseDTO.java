package com.mobflow.chatservice.model.dto.response;

import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.model.enums.MessageContentType;
import com.mobflow.chatservice.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private MessageContentType contentType;
    private MessageStatus status;
    private Set<UUID> readBy;
    private Instant createdAt;
    private boolean ownMessage;
    private boolean readByCurrentUser;

    public static MessageResponseDTO fromEntity(Message message, UUID currentAuthId) {
        Set<UUID> readBy = message.getReadBy() == null ? Set.of() : new LinkedHashSet<>(message.getReadBy());
        boolean hasCurrentUser = currentAuthId != null;

        return MessageResponseDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .contentType(message.getContentType())
                .status(message.getStatus())
                .readBy(readBy)
                .createdAt(message.getCreatedAt())
                .ownMessage(hasCurrentUser && message.getSenderId().equals(currentAuthId))
                .readByCurrentUser(hasCurrentUser && readBy.contains(currentAuthId))
                .build();
    }
}
