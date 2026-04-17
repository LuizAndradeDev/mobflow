package com.mobflow.chatservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.model.dto.websocket.ChatSendMessageRequestDTO;
import com.mobflow.chatservice.repository.ConversationRepository;
import com.mobflow.chatservice.repository.MessageRepository;
import com.mobflow.chatservice.security.AuthenticatedUser;
import com.mobflow.chatservice.service.MessageService;
import com.mobflow.chatservice.testsupport.AbstractChatIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.CONVERSATION_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

class ChatKafkaPublishingIntegrationTest extends AbstractChatIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SocialServiceClient socialServiceClient;

    private Consumer<String, String> chatEventsConsumer;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        chatEventsConsumer = consumer("social.events", "chat-kafka-" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        if (chatEventsConsumer != null) {
            chatEventsConsumer.close(Duration.ofSeconds(1));
        }
    }

    @Test
    void sendMessage_publishesNewChatMessageEvent() throws Exception {
        conversationRepository.save(conversation());
        doNothing().when(socialServiceClient).validateFriendshipRequired(USER_ID, FRIEND_ID);

        messageService.sendMessage(
                AuthenticatedUser.of(USER_ID, "john_dev"),
                ChatSendMessageRequestDTO.builder()
                        .conversationId(CONVERSATION_ID)
                        .content("hello kafka")
                        .build()
        );

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(chatEventsConsumer, "social.events");
        Map<String, Object> payload = objectMapper.readValue(record.value(), Map.class);

        assertThat(payload)
                .containsEntry("eventType", "NEW_CHAT_MESSAGE")
                .containsEntry("senderId", USER_ID.toString())
                .containsEntry("recipientId", FRIEND_ID.toString());
    }
}
