package com.mobflow.chatservice.model.entities;

import com.mobflow.chatservice.model.enums.MessageContentType;
import com.mobflow.chatservice.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Document(collection = "messages")
@CompoundIndex(name = "idx_message_conversation_created", def = "{'conversationId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private UUID id;

    @Indexed
    private UUID conversationId;

    @Indexed
    private UUID senderId;

    private String content;
    private MessageContentType contentType;
    private MessageStatus status;
    private Set<UUID> readBy;

    @CreatedDate
    private Instant createdAt;

    public static Message create(UUID conversationId, UUID senderId, String content, Instant createdAt) {
        Set<UUID> readBy = new LinkedHashSet<>();
        readBy.add(senderId);

        return Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(readBy)
                .createdAt(createdAt)
                .build();
    }
}
