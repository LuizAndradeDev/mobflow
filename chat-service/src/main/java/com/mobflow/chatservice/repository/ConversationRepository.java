package com.mobflow.chatservice.repository;

import com.mobflow.chatservice.model.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends MongoRepository<Conversation, UUID> {

    Optional<Conversation> findByParticipantPairKey(String participantPairKey);

    List<Conversation> findByParticipantIdsContainingOrderByLastActivityAtDesc(UUID participantId);
}
