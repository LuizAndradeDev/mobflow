package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.model.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        applyDefaults(notification);
        return notificationRepository.save(notification);
    }

    public List<NotificationResponseDTO> listNotifications(String recipientId) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    public List<NotificationResponseDTO> listUnreadNotifications(String recipientId) {
        return notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    public long countUnreadNotifications(String recipientId) {
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
    public int markAllAsRead(String recipientId) {
        List<Notification> unreadNotifications =
                notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);

        if (unreadNotifications.isEmpty()) {
            return 0;
        }

        Instant readAt = Instant.now();
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(readAt);
        });
        notificationRepository.saveAll(unreadNotifications);

        return unreadNotifications.size();
    }

    private Notification findByIdAndRecipient(String notificationId, String recipientId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
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
