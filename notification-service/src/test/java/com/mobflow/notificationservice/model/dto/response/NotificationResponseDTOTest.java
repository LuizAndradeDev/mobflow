package com.mobflow.notificationservice.model.dto.response;

import com.mobflow.notificationservice.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseDTOTest {

    @Test
    void fromEntityMapsAllRelevantFields() {
        Instant createdAt = Instant.parse("2026-04-15T15:00:00Z");
        Instant readAt = Instant.parse("2026-04-15T15:30:00Z");

        Notification notification = Notification.builder()
                .id("notification-1")
                .recipientId("user-1")
                .title("Task assigned")
                .body("You were assigned to task ABC-123")
                .type(NotificationType.TASK_UPDATED)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .read(true)
                .createdAt(createdAt)
                .readAt(readAt)
                .metadata(Map.of("taskId", "ABC-123"))
                .build();

        NotificationResponseDTO response = NotificationResponseDTO.fromEntity(notification);

        assertThat(response.id()).isEqualTo("notification-1");
        assertThat(response.type()).isEqualTo(NotificationType.TASK_UPDATED);
        assertThat(response.channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(response.title()).isEqualTo("Task assigned");
        assertThat(response.body()).isEqualTo("You were assigned to task ABC-123");
        assertThat(response.priority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(response.read()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.readAt()).isEqualTo(readAt);
        assertThat(response.metadata()).containsEntry("taskId", "ABC-123");
    }
}
