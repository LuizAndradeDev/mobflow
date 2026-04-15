package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.exception.NotificationNotFoundException;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.repository.NotificationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;

    public NotificationService(NotificationRepository notificationRepository, MongoTemplate mongoTemplate) {
        this.notificationRepository = notificationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public Notification save(Notification notification) {
        applyDefaults(notification);
        return notificationRepository.save(notification);
    }

    public List<NotificationResponseDTO> listForUser(String recipientId) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    public long countUnread(String recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(String notificationId, String recipientId) {
        Notification notification = findByIdAndRecipient(notificationId, recipientId);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }

        return NotificationResponseDTO.fromEntity(notificationRepository.save(notification));
    }

    @Transactional
    public long markAllAsRead(String recipientId) {
        long unread = countUnread(recipientId);
        if (unread == 0) {
            return 0;
        }

        Query query = new Query(Criteria.where("recipientId").is(recipientId).and("read").is(false));
        Update update = new Update()
                .set("read", true)
                .set("readAt", Instant.now())
                .set("updatedAt", Instant.now());
        mongoTemplate.updateMulti(query, update, Notification.class);
        return unread;
    }

    @Transactional
    public void markAsSent(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.setSentAt(Instant.now());
        notification.setDeliveredAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void incrementRetryCount(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        });
    }

    private Notification findByIdAndRecipient(String notificationId, String recipientId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }

    private void applyDefaults(Notification notification) {
        if (notification.getPriority() == null) {
            notification.setPriority(NotificationPriority.MEDIUM);
        }
        if (notification.getChannel() == null) {
            notification.setChannel(NotificationChannel.IN_APP);
        }
        if (notification.getMaxRetries() <= 0) {
            notification.setMaxRetries(DEFAULT_MAX_RETRIES);
        }
        if (notification.getRetryCount() < 0) {
            notification.setRetryCount(0);
        }
    }
}
