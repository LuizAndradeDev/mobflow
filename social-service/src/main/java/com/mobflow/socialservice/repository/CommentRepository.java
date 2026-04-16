package com.mobflow.socialservice.repository;

import com.mobflow.socialservice.model.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface CommentRepository extends MongoRepository<Comment, UUID> {

    Page<Comment> findByTaskId(UUID taskId, Pageable pageable);
}
