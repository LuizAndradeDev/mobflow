package com.mobflow.chatservice.repository;

import com.mobflow.chatservice.model.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface MessageRepository extends MongoRepository<Message, UUID> {

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);
}
