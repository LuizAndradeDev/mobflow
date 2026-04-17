package com.mobflow.chatservice.service;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.kafka.events.NewChatMessageEvent;
import com.mobflow.chatservice.kafka.producers.ChatEventProducer;
import com.mobflow.chatservice.model.dto.response.MarkConversationReadResponseDTO;
import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.repository.MessageRepository;
import com.mobflow.chatservice.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.CONVERSATION_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.message;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.sendMessageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationGuardService conversationGuardService;

    @Mock
    private ConversationReadService conversationReadService;

    @Mock
    private SocialServiceClient socialServiceClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ChatRealtimeNotifier chatRealtimeNotifier;

    @Mock
    private ChatEventProducer chatEventProducer;

    @InjectMocks
    private MessageService messageService;

    @Test
    void listMessages_marksConversationAsReadAndReturnsPage() {
        Conversation existingConversation = conversation();
        Message existingMessage = message();

        when(conversationGuardService.requireParticipant(CONVERSATION_ID, USER_ID)).thenReturn(existingConversation);
        when(conversationReadService.markConversationAsRead(existingConversation, USER_ID))
                .thenReturn(MarkConversationReadResponseDTO.builder()
                        .conversationId(CONVERSATION_ID)
                        .markedCount(1L)
                        .unreadCount(0L)
                        .readAt(Instant.now())
                        .build());
        when(messageRepository.findByConversationId(eq(CONVERSATION_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingMessage)));

        Page<MessageResponseDTO> response = messageService.listMessages(
                CONVERSATION_ID,
                AuthenticatedUser.of(USER_ID, "john_dev"),
                0,
                20
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getSenderId()).isEqualTo(FRIEND_ID);
        verify(conversationReadService).markConversationAsRead(existingConversation, USER_ID);
    }

    @Test
    void sendMessage_blankContent_throwsBusinessException() {
        assertThatThrownBy(() -> messageService.sendMessage(
                AuthenticatedUser.of(USER_ID, "john_dev"),
                com.mobflow.chatservice.model.dto.websocket.ChatSendMessageRequestDTO.builder()
                        .conversationId(CONVERSATION_ID)
                        .content("   ")
                        .build()
        )).isInstanceOf(ChatServiceException.class)
                .hasMessage("Message content must not be blank");
    }

    @Test
    void sendMessage_validMessage_persistsUpdatesConversationAndPublishesEvents() {
        Conversation existingConversation = conversation();
        Conversation updatedConversation = conversation();
        updatedConversation.getUnreadCountByUser().put(FRIEND_ID.toString(), 1L);
        Message savedMessage = message();

        when(conversationGuardService.requireParticipant(CONVERSATION_ID, USER_ID)).thenReturn(existingConversation);
        when(conversationGuardService.resolveCounterpartAuthId(existingConversation, USER_ID)).thenReturn(FRIEND_ID);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Conversation.class)))
                .thenReturn(updatedConversation);

        messageService.sendMessage(AuthenticatedUser.of(USER_ID, "john_dev"), sendMessageRequest());

        ArgumentCaptor<NewChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(NewChatMessageEvent.class);

        verify(socialServiceClient).validateFriendshipRequired(USER_ID, FRIEND_ID);
        verify(chatRealtimeNotifier).publishMessageCreated(any(Conversation.class), any(MessageResponseDTO.class), any(MessageResponseDTO.class), any(MessageResponseDTO.class));
        verify(chatEventProducer).publishNewChatMessage(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRecipientId()).isEqualTo(FRIEND_ID.toString());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("NEW_CHAT_MESSAGE");
    }
}
