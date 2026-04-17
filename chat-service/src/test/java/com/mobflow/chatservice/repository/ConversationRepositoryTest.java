package com.mobflow.chatservice.repository;

import com.mobflow.chatservice.config.MongoConfig;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.testsupport.AbstractMongoChatTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.OUTSIDER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
class ConversationRepositoryTest extends AbstractMongoChatTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @BeforeEach
    void cleanUp() {
        conversationRepository.deleteAll();
    }

    @Test
    void save_duplicateParticipantPairKey_throwsDuplicateKeyException() {
        conversationRepository.save(conversation());

        Conversation duplicateConversation = conversation();
        duplicateConversation.setId(UUID.randomUUID());

        assertThatThrownBy(() -> conversationRepository.save(duplicateConversation))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void findByParticipantIdsContainingOrderByLastActivityAtDesc_returnsNewestConversationFirst() {
        Conversation newestConversation = conversation();
        newestConversation.setLastActivityAt(Instant.parse("2026-04-17T11:00:00Z"));

        Map<String, Long> unreadCountByUser = new LinkedHashMap<>();
        unreadCountByUser.put(USER_ID.toString(), 0L);
        unreadCountByUser.put(OUTSIDER_ID.toString(), 0L);

        Conversation olderConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .participantIds(List.of(USER_ID, OUTSIDER_ID))
                .participantPairKey(FRIEND_ID + ":" + OUTSIDER_ID)
                .type(com.mobflow.chatservice.model.enums.ConversationType.PRIVATE)
                .lastActivityAt(Instant.parse("2026-04-17T09:00:00Z"))
                .unreadCountByUser(unreadCountByUser)
                .createdAt(Instant.parse("2026-04-17T09:00:00Z"))
                .updatedAt(Instant.parse("2026-04-17T09:00:00Z"))
                .build();

        conversationRepository.saveAll(List.of(olderConversation, newestConversation));

        List<Conversation> conversations = conversationRepository.findByParticipantIdsContainingOrderByLastActivityAtDesc(USER_ID);

        assertThat(conversations).hasSize(2);
        assertThat(conversations.getFirst().getId()).isEqualTo(newestConversation.getId());
    }
}
