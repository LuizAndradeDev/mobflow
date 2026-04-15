package com.mobflow.notificationservice.repository;

import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.testsupport.AbstractMongoNotificationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.Instant;
import java.util.List;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.notification;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class NotificationRepositoryTest extends AbstractMongoNotificationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void findAllByRecipientIdOrderByCreatedAtDesc_existingNotifications_returnsNewestFirst() {
        Notification older = notification("user-1");
        older.setCreatedAt(Instant.parse("2026-04-14T10:00:00Z"));
        Notification newer = notification("user-1");
        newer.setCreatedAt(Instant.parse("2026-04-15T10:00:00Z"));
        notificationRepository.saveAll(List.of(older, newer));

        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc("user-1"))
                .extracting(Notification::getCreatedAt)
                .containsExactly(newer.getCreatedAt(), older.getCreatedAt());
    }

    @Test
    void countByRecipientIdAndReadFalse_existingUnreadNotifications_returnsOnlyUnreadCount() {
        Notification unread = notification("user-1");
        Notification read = notification("user-1");
        read.setRead(true);
        notificationRepository.saveAll(List.of(unread, read));

        assertThat(notificationRepository.countByRecipientIdAndReadFalse("user-1")).isEqualTo(1L);
    }

    @Test
    void findByIdAndRecipientId_matchingNotification_returnsOwnedNotification() {
        Notification notification = notificationRepository.save(notification("user-1"));

        assertThat(notificationRepository.findByIdAndRecipientId(notification.getId(), "user-1")).isPresent();
    }
}
