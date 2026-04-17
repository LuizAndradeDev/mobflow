package com.mobflow.chatservice.service;

import com.mobflow.chatservice.model.dto.response.MarkConversationReadResponseDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.enums.MessageStatus;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationReadService {

    private final MongoTemplate mongoTemplate;
    private final ChatRealtimeNotifier chatRealtimeNotifier;

    public MarkConversationReadResponseDTO markConversationAsRead(Conversation conversation, UUID currentAuthId) {
        Query unreadMessagesQuery = new Query(Criteria.where("conversationId").is(conversation.getId())
                .and("senderId").ne(currentAuthId)
                .and("readBy").nin(currentAuthId));

        Update markMessagesAsReadUpdate = new Update()
                .addToSet("readBy", currentAuthId)
                .set("status", MessageStatus.READ);

        UpdateResult updateResult = mongoTemplate.updateMulti(
                unreadMessagesQuery,
                markMessagesAsReadUpdate,
                com.mobflow.chatservice.model.entities.Message.class
        );

        long markedCount = updateResult.getModifiedCount();
        Instant readAt = Instant.now();

        if (markedCount > 0 || conversation.getUnreadCountByUser().getOrDefault(currentAuthId.toString(), 0L) > 0) {
            Query conversationQuery = new Query(Criteria.where("_id").is(conversation.getId()));
            Update conversationUpdate = new Update()
                    .set("unreadCountByUser." + currentAuthId, 0L)
                    .set("updatedAt", readAt);

            mongoTemplate.updateFirst(conversationQuery, conversationUpdate, Conversation.class);
            conversation.getUnreadCountByUser().put(currentAuthId.toString(), 0L);
            conversation.setUpdatedAt(readAt);
        }

        chatRealtimeNotifier.publishConversationRead(conversation, currentAuthId, markedCount, readAt);

        return MarkConversationReadResponseDTO.builder()
                .conversationId(conversation.getId())
                .markedCount(markedCount)
                .unreadCount(0L)
                .readAt(readAt)
                .build();
    }
}
