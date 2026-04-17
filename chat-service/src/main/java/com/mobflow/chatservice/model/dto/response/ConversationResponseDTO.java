package com.mobflow.chatservice.model.dto.response;

import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {

    private UUID id;
    private ConversationType type;
    private List<UUID> participantIds;
    private UUID counterpartAuthId;
    private ConversationLastMessageResponseDTO lastMessage;
    private Instant lastActivityAt;
    private long unreadCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationResponseDTO fromEntity(Conversation conversation, UUID currentAuthId) {
        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .participantIds(conversation.getParticipantIds())
                .counterpartAuthId(conversation.getParticipantIds().stream()
                        .filter(participantId -> !participantId.equals(currentAuthId))
                        .findFirst()
                        .orElse(currentAuthId))
                .lastMessage(ConversationLastMessageResponseDTO.fromEntity(conversation.getLastMessage()))
                .lastActivityAt(conversation.getLastActivityAt())
                .unreadCount(conversation.getUnreadCountByUser().getOrDefault(currentAuthId.toString(), 0L))
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
