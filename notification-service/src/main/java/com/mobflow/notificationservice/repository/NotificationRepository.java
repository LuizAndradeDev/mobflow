package com.mobflow.notificationservice.repository;

import com.mobflow.notificationservice.model.entities.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId);
    List<Notification> findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId);
    Optional<Notification> findByIdAndRecipientId(String id, String recipientId);
    long countByRecipientIdAndReadFalse(String recipientId);
}
