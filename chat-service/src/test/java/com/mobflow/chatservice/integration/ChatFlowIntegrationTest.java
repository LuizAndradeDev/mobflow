package com.mobflow.chatservice.integration;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.model.enums.MessageContentType;
import com.mobflow.chatservice.model.enums.MessageStatus;
import com.mobflow.chatservice.repository.ConversationRepository;
import com.mobflow.chatservice.repository.MessageRepository;
import com.mobflow.chatservice.testsupport.AbstractChatIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.CONVERSATION_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatFlowIntegrationTest extends AbstractChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockBean
    private SocialServiceClient socialServiceClient;

    @BeforeEach
    void cleanUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    void createConversation_thenListConversations_returnsPersistedConversation() throws Exception {
        doNothing().when(socialServiceClient).validateFriendshipRequired(USER_ID, FRIEND_ID);

        mockMvc.perform(withChatContextPath(post("/chat/api/conversations/private"))
                        .header("Authorization", bearerToken(USER_ID, "john_dev"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetAuthId": "%s"
                                }
                                """.formatted(FRIEND_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true));

        mockMvc.perform(withChatContextPath(get("/chat/api/conversations"))
                        .header("Authorization", bearerToken(USER_ID, "john_dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counterpartAuthId").value(FRIEND_ID.toString()))
                .andExpect(jsonPath("$[0].unreadCount").value(0));

        assertThat(conversationRepository.findAll()).hasSize(1);
    }

    @Test
    void listMessages_marksUnreadMessagesAsReadAndResetsUnreadCount() throws Exception {
        Conversation storedConversation = conversation();
        storedConversation.getUnreadCountByUser().put(USER_ID.toString(), 2L);
        conversationRepository.save(storedConversation);

        Message firstUnreadMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(CONVERSATION_ID)
                .senderId(FRIEND_ID)
                .content("first")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(new LinkedHashSet<>(Set.of(FRIEND_ID)))
                .createdAt(Instant.parse("2026-04-17T10:00:00Z"))
                .build();

        Message secondUnreadMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(CONVERSATION_ID)
                .senderId(FRIEND_ID)
                .content("second")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(new LinkedHashSet<>(Set.of(FRIEND_ID)))
                .createdAt(Instant.parse("2026-04-17T10:01:00Z"))
                .build();

        messageRepository.saveAll(List.of(firstUnreadMessage, secondUnreadMessage));

        mockMvc.perform(withChatContextPath(get("/chat/api/conversations/{conversationId}/messages", CONVERSATION_ID))
                        .header("Authorization", bearerToken(USER_ID, "john_dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("READ"))
                .andExpect(jsonPath("$.content[1].status").value("READ"));

        Conversation updatedConversation = conversationRepository.findById(CONVERSATION_ID).orElseThrow();
        List<Message> updatedMessages = messageRepository.findAll();

        assertThat(updatedConversation.getUnreadCountByUser().get(USER_ID.toString())).isZero();
        assertThat(updatedMessages)
                .allSatisfy(message -> {
                    assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);
                    assertThat(message.getReadBy()).contains(USER_ID);
                });
    }
}
