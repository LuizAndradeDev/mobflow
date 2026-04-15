package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.model.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;
import com.mobflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void createNotificationAppliesDefaultsBeforeSaving() {
        Notification notification = Notification.builder()
                .recipientId("user-1")
                .title("Task created")
                .body("A new task was created")
                .type(NotificationType.TASK_CREATE)
                .retryCount(-1)
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.createNotification(notification);

        assertThat(saved.getPriority()).isEqualTo(NotificationPriority.MEDIUM);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getMaxRetries()).isEqualTo(3);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void listNotificationsMapsRepositoryResultsToResponseDtos() {
        Notification first = notification("notification-1", false, null, Instant.parse("2026-04-15T10:00:00Z"));
        Notification second = notification("notification-2", true, Instant.parse("2026-04-15T11:00:00Z"),
                Instant.parse("2026-04-15T09:00:00Z"));

        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(first, second));

        List<NotificationResponseDTO> responses = notificationService.listNotifications("user-1");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(NotificationResponseDTO::id)
                .containsExactly("notification-1", "notification-2");
        assertThat(responses.get(1).read()).isTrue();
        assertThat(responses.get(1).readAt()).isEqualTo(second.getReadAt());
    }

    @Test
    void listUnreadNotificationsReturnsOnlyUnreadDtos() {
        Notification unread = notification("notification-1", false, null, Instant.parse("2026-04-15T10:00:00Z"));

        when(notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(unread));

        List<NotificationResponseDTO> responses = notificationService.listUnreadNotifications("user-1");

        assertThat(responses).singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo("notification-1");
                    assertThat(response.read()).isFalse();
                });
    }

    @Test
    void countUnreadNotificationsDelegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse("user-1")).thenReturn(4L);

        long unreadCount = notificationService.countUnreadNotifications("user-1");

        assertThat(unreadCount).isEqualTo(4L);
    }

    @Test
    void markAsReadUpdatesUnreadNotificationAndPersistsIt() {
        Notification notification = notification("notification-1", false, null, Instant.parse("2026-04-15T10:00:00Z"));

        when(notificationRepository.findByIdAndRecipientId("notification-1", "user-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.markAsRead("notification-1", "user-1");

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadKeepsExistingReadTimestampWhenAlreadyRead() {
        Instant readAt = Instant.parse("2026-04-15T10:15:00Z");
        Notification notification = notification("notification-1", true, readAt, Instant.parse("2026-04-15T10:00:00Z"));

        when(notificationRepository.findByIdAndRecipientId("notification-1", "user-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.markAsRead("notification-1", "user-1");

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isEqualTo(readAt);
    }

    @Test
    void markAsReadThrowsWhenNotificationDoesNotBelongToRecipient() {
        when(notificationRepository.findByIdAndRecipientId("notification-1", "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("notification-1", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(NOT_FOUND));

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllAsReadUpdatesEveryUnreadNotificationInSingleBatch() {
        Notification first = notification("notification-1", false, null, Instant.parse("2026-04-15T10:00:00Z"));
        Notification second = notification("notification-2", false, null, Instant.parse("2026-04-15T09:00:00Z"));

        when(notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(first, second));

        int updatedCount = notificationService.markAllAsRead("user-1");

        assertThat(updatedCount).isEqualTo(2);
        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        assertThat(first.getReadAt()).isNotNull();
        assertThat(second.getReadAt()).isEqualTo(first.getReadAt());

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(first, second);
    }

    @Test
    void markAllAsReadReturnsZeroWhenThereAreNoUnreadNotifications() {
        when(notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of());

        int updatedCount = notificationService.markAllAsRead("user-1");

        assertThat(updatedCount).isZero();
        verify(notificationRepository, never()).saveAll(anyList());
    }

    private Notification notification(String id, boolean read, Instant readAt, Instant createdAt) {
        return Notification.builder()
                .id(id)
                .recipientId("user-1")
                .recipientEmail("user-1@mobflow.dev")
                .title("Notification " + id)
                .body("Body for " + id)
                .type(NotificationType.TASK_UPDATE)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .read(read)
                .readAt(readAt)
                .createdAt(createdAt)
                .metadata(Map.of("source", "test"))
                .build();
    }
}
