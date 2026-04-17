package com.mobflow.chatservice.service;

import com.mobflow.chatservice.model.dto.response.ConversationResponseDTO;
import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.model.dto.websocket.ConversationReadReceiptDTO;
import com.mobflow.chatservice.model.dto.websocket.ConversationTopicEventDTO;
import com.mobflow.chatservice.model.dto.websocket.UserQueueEventDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.enums.ChatEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationGuardService conversationGuardService;

    public void publishMessageCreated(
            Conversation conversation,
            MessageResponseDTO conversationEventMessage,
            MessageResponseDTO senderView,
            MessageResponseDTO recipientView
    ) {
        ConversationTopicEventDTO topicEvent = ConversationTopicEventDTO.builder()
                .eventType(ChatEventType.MESSAGE_CREATED)
                .conversationId(conversation.getId())
                .message(conversationEventMessage)
                .occurredAt(conversationEventMessage.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), topicEvent);

        UUID senderId = conversationEventMessage.getSenderId();
        UUID recipientId = conversationGuardService.resolveCounterpartAuthId(conversation, senderId);

        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                UserQueueEventDTO.builder()
                        .eventType(ChatEventType.MESSAGE_CREATED)
                        .conversation(ConversationResponseDTO.fromEntity(conversation, senderId))
                        .message(senderView)
                        .occurredAt(senderView.getCreatedAt())
                        .build()
        );

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                UserQueueEventDTO.builder()
                        .eventType(ChatEventType.MESSAGE_CREATED)
                        .conversation(ConversationResponseDTO.fromEntity(conversation, recipientId))
                        .message(recipientView)
                        .occurredAt(recipientView.getCreatedAt())
                        .build()
        );
    }

    public void publishConversationRead(Conversation conversation, UUID readerAuthId, long readCount, Instant readAt) {
        if (readCount <= 0) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getId(),
                ConversationTopicEventDTO.builder()
                        .eventType(ChatEventType.CONVERSATION_READ)
                        .conversationId(conversation.getId())
                        .readReceipt(ConversationReadReceiptDTO.builder()
                                .conversationId(conversation.getId())
                                .readerAuthId(readerAuthId)
                                .readCount(readCount)
                                .readAt(readAt)
                                .build())
                        .occurredAt(readAt)
                        .build()
        );

        messagingTemplate.convertAndSendToUser(
                readerAuthId.toString(),
                "/queue/messages",
                UserQueueEventDTO.builder()
                        .eventType(ChatEventType.CONVERSATION_READ)
                        .conversation(ConversationResponseDTO.fromEntity(conversation, readerAuthId))
                        .occurredAt(readAt)
                        .build()
        );
    }
}
