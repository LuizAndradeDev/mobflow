package com.mobflow.chatservice.model.entities;

import com.mobflow.chatservice.model.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "conversations")
@CompoundIndex(name = "idx_conversation_participant_activity", def = "{'participantIds': 1, 'lastActivityAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private UUID id;

    @Indexed
    private List<UUID> participantIds;

    @Indexed(unique = true)
    private String participantPairKey;

    private ConversationType type;
    private ConversationLastMessage lastMessage;

    @Indexed
    private Instant lastActivityAt;

    private Map<String, Long> unreadCountByUser;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public static Conversation create(
            List<UUID> participantIds,
            String participantPairKey,
            Map<String, Long> unreadCountByUser,
            Instant createdAt
    ) {
        return Conversation.builder()
                .id(UUID.randomUUID())
                .participantIds(participantIds)
                .participantPairKey(participantPairKey)
                .type(ConversationType.PRIVATE)
                .lastActivityAt(createdAt)
                .unreadCountByUser(unreadCountByUser)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
