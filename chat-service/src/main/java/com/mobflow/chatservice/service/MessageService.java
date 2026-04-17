package com.mobflow.chatservice.service;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.kafka.events.NewChatMessageEvent;
import com.mobflow.chatservice.kafka.producers.ChatEventProducer;
import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.model.dto.websocket.ChatSendMessageRequestDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.entities.ConversationLastMessage;
import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.repository.MessageRepository;
import com.mobflow.chatservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationGuardService conversationGuardService;
    private final ConversationReadService conversationReadService;
    private final SocialServiceClient socialServiceClient;
    private final MongoTemplate mongoTemplate;
    private final ChatRealtimeNotifier chatRealtimeNotifier;
    private final ChatEventProducer chatEventProducer;

    public Page<MessageResponseDTO> listMessages(
            UUID conversationId,
            AuthenticatedUser authenticatedUser,
            int page,
            int size
    ) {
        Conversation conversation = conversationGuardService.requireParticipant(conversationId, authenticatedUser.authId());
        conversationReadService.markConversationAsRead(conversation, authenticatedUser.authId());

        return messageRepository.findByConversationId(
                        conversationId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).map(message -> MessageResponseDTO.fromEntity(message, authenticatedUser.authId()));
    }

    public void sendMessage(AuthenticatedUser authenticatedUser, ChatSendMessageRequestDTO request) {
        String normalizedContent = request.getContent() == null ? "" : request.getContent().trim();
        if (normalizedContent.isBlank()) {
            throw ChatServiceException.invalidMessageContent();
        }

        Conversation conversation = conversationGuardService.requireParticipant(request.getConversationId(), authenticatedUser.authId());
        UUID recipientAuthId = conversationGuardService.resolveCounterpartAuthId(conversation, authenticatedUser.authId());

        socialServiceClient.validateFriendshipRequired(authenticatedUser.authId(), recipientAuthId);

        Instant now = Instant.now();
        Message message = Message.create(conversation.getId(), authenticatedUser.authId(), normalizedContent, now);
        Message savedMessage = messageRepository.save(message);

        Conversation updatedConversation = mongoTemplate.findAndModify(
                new Query(Criteria.where("_id").is(conversation.getId())),
                new Update()
                        .set("lastMessage", ConversationLastMessage.fromMessage(savedMessage))
                        .set("lastActivityAt", savedMessage.getCreatedAt())
                        .set("updatedAt", savedMessage.getCreatedAt())
                        .set("unreadCountByUser." + authenticatedUser.authId(), 0L)
                        .inc("unreadCountByUser." + recipientAuthId, 1L),
                FindAndModifyOptions.options().returnNew(true),
                Conversation.class
        );

        if (updatedConversation == null) {
            throw ChatServiceException.conversationNotFound();
        }

        MessageResponseDTO conversationEventMessage = MessageResponseDTO.fromEntity(savedMessage, null);
        MessageResponseDTO senderView = MessageResponseDTO.fromEntity(savedMessage, authenticatedUser.authId());
        MessageResponseDTO recipientView = MessageResponseDTO.fromEntity(savedMessage, recipientAuthId);

        chatRealtimeNotifier.publishMessageCreated(updatedConversation, conversationEventMessage, senderView, recipientView);
        chatEventProducer.publishNewChatMessage(NewChatMessageEvent.builder()
                .eventType("NEW_CHAT_MESSAGE")
                .messageId(savedMessage.getId().toString())
                .conversationId(updatedConversation.getId().toString())
                .senderId(authenticatedUser.authId().toString())
                .recipientId(recipientAuthId.toString())
                .contentPreview(savedMessage.getContent())
                .createdAt(savedMessage.getCreatedAt())
                .build());
    }
}
