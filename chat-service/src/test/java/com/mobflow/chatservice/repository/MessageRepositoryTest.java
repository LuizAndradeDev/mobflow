package com.mobflow.chatservice.repository;

import com.mobflow.chatservice.config.MongoConfig;
import com.mobflow.chatservice.model.entities.Message;
import com.mobflow.chatservice.model.enums.MessageContentType;
import com.mobflow.chatservice.model.enums.MessageStatus;
import com.mobflow.chatservice.testsupport.AbstractMongoChatTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.CONVERSATION_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.MESSAGE_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
class MessageRepositoryTest extends AbstractMongoChatTest {

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void cleanUp() {
        messageRepository.deleteAll();
    }

    @Test
    void findByConversationId_withDescendingSort_returnsLatestMessageFirst() {
        Message olderMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(CONVERSATION_ID)
                .senderId(FRIEND_ID)
                .content("older")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(Set.of(FRIEND_ID))
                .createdAt(Instant.parse("2026-04-17T09:00:00Z"))
                .build();

        Message newerMessage = Message.builder()
                .id(MESSAGE_ID)
                .conversationId(CONVERSATION_ID)
                .senderId(USER_ID)
                .content("newer")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .readBy(Set.of(USER_ID))
                .createdAt(Instant.parse("2026-04-17T10:00:00Z"))
                .build();

        messageRepository.saveAll(List.of(olderMessage, newerMessage));

        Page<Message> page = messageRepository.findByConversationId(
                CONVERSATION_ID,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(MESSAGE_ID);
    }
}
