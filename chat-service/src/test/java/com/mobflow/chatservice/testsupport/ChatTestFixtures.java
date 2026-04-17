package com.mobflow.chatservice.testsupport;

import com.mobflow.chatservice.model.dto.request.CreateConversationRequestDTO;
import com.mobflow.chatservice.model.dto.websocket.ChatSendMessageRequestDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.entities.ConversationLastMessage;
import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.model.enums.ConversationType;
import com.mobflow.chatservice.model.enums.MessageContentType;
import com.mobflow.chatservice.model.enums.MessageStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatTestFixtures {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID FRIEND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID OUTSIDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID CONVERSATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID MESSAGE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private ChatTestFixtures() {
    }

    public static CreateConversationRequestDTO createConversationRequest() {
        return CreateConversationRequestDTO.builder()
                .targetAuthId(FRIEND_ID)
                .build();
    }

    public static ChatSendMessageRequestDTO sendMessageRequest() {
        return ChatSendMessageRequestDTO.builder()
                .conversationId(CONVERSATION_ID)
                .content("hello there")
                .build();
    }

    public static Conversation conversation() {
        Instant now = Instant.parse("2026-04-17T10:00:00Z");
        Map<String, Long> unreadCountByUser = new LinkedHashMap<>();
        unreadCountByUser.put(USER_ID.toString(), 0L);
        unreadCountByUser.put(FRIEND_ID.toString(), 0L);

        return Conversation.builder()
                .id(CONVERSATION_ID)
                .participantIds(List.of(USER_ID, FRIEND_ID))
                .participantPairKey(USER_ID.toString() + ":" + FRIEND_ID.toString())
                .type(ConversationType.PRIVATE)
                .lastActivityAt(now)
                .unreadCountByUser(unreadCountByUser)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Conversation conversationWithUnread(long unreadCount) {
        Conversation conversation = conversation();
        conversation.getUnreadCountByUser().put(USER_ID.toString(), unreadCount);
        return conversation;
    }

    public static Conversation conversationWithLastMessage() {
        Conversation conversation = conversation();
        conversation.setLastMessage(ConversationLastMessage.builder()
                .messageId(MESSAGE_ID)
                .senderId(FRIEND_ID)
                .contentPreview("latest message")
                .contentType(MessageContentType.TEXT)
                .createdAt(Instant.parse("2026-04-17T10:05:00Z"))
                .build());
        conversation.setLastActivityAt(Instant.parse("2026-04-17T10:05:00Z"));
        conversation.setUnreadCountByUser(new LinkedHashMap<>(Map.of(
                USER_ID.toString(), 2L,
                FRIEND_ID.toString(), 0L
        )));
        return conversation;
    }

    public static Message message() {
        Set<UUID> readBy = new LinkedHashSet<>();
        readBy.add(FRIEND_ID);

        return Message.builder()
                .id(MESSAGE_ID)
                .conversationId(CONVERSATION_ID)
                .senderId(FRIEND_ID)
                .content("hello there")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(readBy)
                .createdAt(Instant.parse("2026-04-17T10:06:00Z"))
                .build();
    }
}
