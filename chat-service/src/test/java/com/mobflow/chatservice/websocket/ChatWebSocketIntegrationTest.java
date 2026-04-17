package com.mobflow.chatservice.websocket;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.model.dto.websocket.ChatSendMessageRequestDTO;
import com.mobflow.chatservice.repository.ConversationRepository;
import com.mobflow.chatservice.repository.MessageRepository;
import com.mobflow.chatservice.service.ChatRealtimeNotifier;
import com.mobflow.chatservice.testsupport.AbstractChatIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.CONVERSATION_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatWebSocketIntegrationTest extends AbstractChatIntegrationTest {

    @Autowired
    private ChatInboundChannelInterceptor chatInboundChannelInterceptor;

    @Autowired
    private ChatWebSocketController chatWebSocketController;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @SpyBean
    private ChatRealtimeNotifier chatRealtimeNotifier;

    @MockBean
    private SocialServiceClient socialServiceClient;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        conversationRepository.save(conversation());
        doNothing().when(socialServiceClient).validateFriendshipRequired(USER_ID, FRIEND_ID);
    }

    @Test
    void connectWithAuthorizationHeader_setsAuthenticatedPrincipalOnSession() {
        Message<byte[]> connectMessage = messageForConnect("session-1", bearerToken(USER_ID, "john_dev"));

        Message<?> interceptedMessage = chatInboundChannelInterceptor.preSend(connectMessage, mock(MessageChannel.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(interceptedMessage);

        assertThat(accessor.getUser()).isInstanceOf(ChatPrincipal.class);
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();
        assertThat(principal.getAuthId()).isEqualTo(USER_ID);
        assertThat(accessor.getSessionAttributes()).containsEntry("authId", USER_ID.toString());
        assertThat(accessor.getSessionAttributes()).containsEntry("username", "john_dev");
    }

    @Test
    void connectWithoutAuthorizationHeader_throwsBusinessException() {
        Message<byte[]> connectMessage = messageForConnect("session-1", null);

        assertThatThrownBy(() -> chatInboundChannelInterceptor.preSend(connectMessage, mock(MessageChannel.class)))
                .isInstanceOf(ChatServiceException.class)
                .hasMessage("A valid JWT is required to establish the WebSocket session");
    }

    @Test
    void sendMessageThroughWebSocketController_persistsMessageAndTriggersRealtimeDispatch() {
        ChatPrincipal principal = new ChatPrincipal(USER_ID, "john_dev");

        chatWebSocketController.sendMessage(
                ChatSendMessageRequestDTO.builder()
                        .conversationId(CONVERSATION_ID)
                        .content("hello websocket")
                        .build(),
                principal
        );

        ArgumentCaptor<com.mobflow.chatservice.model.entities.Conversation> conversationCaptor =
                ArgumentCaptor.forClass(com.mobflow.chatservice.model.entities.Conversation.class);

        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(conversationRepository.findById(CONVERSATION_ID).orElseThrow()
                .getUnreadCountByUser().get(FRIEND_ID.toString())).isEqualTo(1L);
        verify(chatRealtimeNotifier).publishMessageCreated(
                conversationCaptor.capture(),
                any(),
                any(),
                any()
        );
        assertThat(conversationCaptor.getValue().getId()).isEqualTo(CONVERSATION_ID);
    }

    @Test
    void subscribeToConversationOutsideParticipantScope_throwsAccessDenied() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("authId", USER_ID.toString());
        sessionAttributes.put("username", "john_dev");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-2");
        accessor.setDestination("/topic/conversations/" + java.util.UUID.randomUUID());
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);

        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> chatInboundChannelInterceptor.preSend(subscribeMessage, mock(MessageChannel.class)))
                .isInstanceOf(ChatServiceException.class);
    }

    private Message<byte[]> messageForConnect(String sessionId, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);

        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(new byte[0], headers);
    }
}
