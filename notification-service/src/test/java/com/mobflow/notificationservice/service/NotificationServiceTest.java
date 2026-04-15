package com.mobflow.notificationservice.service;

import com.mongodb.client.result.UpdateResult;
import com.mobflow.notificationservice.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.exception.NotificationNotFoundException;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, mongoTemplate);
    }

    @Test
    void save_missingDefaults_appliesExpectedDefaultsBeforePersisting() {
        Notification notification = Notification.builder()
                .recipientId("user-1")
                .title("Task update")
                .body("A task changed")
                .retryCount(-1)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.save(notification);

        assertThat(saved.getPriority()).isEqualTo(NotificationPriority.MEDIUM);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void listForUser_existingNotifications_returnsMappedDtos() {
        Notification first = notification("user-1");
        Notification second = notification("user-1");
        second.setRead(true);
        second.setReadAt(Instant.now());

        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(first, second));

        List<NotificationResponseDTO> responses = notificationService.listForUser("user-1");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(1).read()).isTrue();
    }

    @Test
    void countUnread_existingUnreadNotifications_returnsRepositoryCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse("user-1")).thenReturn(4L);

        assertThat(notificationService.countUnread("user-1")).isEqualTo(4L);
    }

    @Test
    void markAsRead_unreadNotification_setsReadFlagsAndPersists() {
        Notification notification = notification("user-1");
        when(notificationRepository.findByIdAndRecipientId(notification.getId(), "user-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponseDTO response = notificationService.markAsRead(notification.getId(), "user-1");

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void markAsRead_missingNotification_throwsNotFound() {
        when(notificationRepository.findByIdAndRecipientId("missing", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("missing", "user-1"))
                .isInstanceOf(NotificationNotFoundException.class);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_existingUnreadNotifications_executesBulkMongoUpdate() {
        when(notificationRepository.countByRecipientIdAndReadFalse("user-1")).thenReturn(2L);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Notification.class)))
                .thenReturn(UpdateResult.acknowledged(2L, 2L, null));

        long updated = notificationService.markAllAsRead("user-1");

        assertThat(updated).isEqualTo(2L);
        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(Notification.class));
    }

    @Test
    void markAsSent_existingNotification_setsDeliveryTimestamps() {
        Notification notification = notification("user-1");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.markAsSent(notification.getId());

        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getDeliveredAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void incrementRetryCount_existingNotification_incrementsPersistedValue() {
        Notification notification = notification("user-1");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.incrementRetryCount(notification.getId());

        assertThat(notification.getRetryCount()).isEqualTo(1);
        verify(notificationRepository).save(notification);
    }
}
