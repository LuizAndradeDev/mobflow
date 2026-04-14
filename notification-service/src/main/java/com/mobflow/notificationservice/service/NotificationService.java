package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.repository.NotificationRepository;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class NotificationService {
    private NotificationRepository notificationRepository;

    public Notification createNotification(Notification notification) {

        return notificationRepository.save(notification);
    }
}
