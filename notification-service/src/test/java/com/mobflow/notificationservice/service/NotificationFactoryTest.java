package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.kafka.events.AuthNotificationEvent;
import com.mobflow.notificationservice.kafka.events.CommentNotificationEvent;
import com.mobflow.notificationservice.kafka.events.TaskNotificationEvent;
import com.mobflow.notificationservice.kafka.events.WorkspaceNotificationEvent;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.authEvent;
import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.commentEvent;
import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.taskEvent;
import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.workspaceEvent;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationFactoryTest {

    private NotificationFactory notificationFactory;

    @BeforeEach
    void setUp() {
        notificationFactory = new NotificationFactory();
    }

    @Test
    void createAuthNotification_emailConfirmationEvent_buildsHighPriorityEmailNotification() {
        AuthNotificationEvent event = authEvent();

        Notification notification = notificationFactory.createAuthNotification(event);

        assertThat(notification.getType()).isEqualTo(NotificationType.EMAIL_CONFIRMATION);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(notification.getMetadata()).containsEntry("confirmationToken", event.confirmationToken());
    }

    @Test
    void createTaskNotification_dueSoonEvent_buildsHighPriorityInAppNotification() {
        TaskNotificationEvent event = taskEvent("TASK_DUE_SOON");

        Notification notification = notificationFactory.createTaskNotification(event);

        assertThat(notification.getType()).isEqualTo(NotificationType.TASK_DUE_SOON);
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(notification.getBody()).contains("is due on");
        assertThat(notification.getMetadata()).containsEntry("boardId", event.boardId());
    }

    @Test
    void createWorkspaceNotification_inviteEvent_buildsInviteNotification() {
        WorkspaceNotificationEvent event = workspaceEvent("WORKSPACE_INVITE");

        Notification notification = notificationFactory.createWorkspaceNotification(event);

        assertThat(notification.getType()).isEqualTo(NotificationType.WORKSPACE_INVITE);
        assertThat(notification.getTitle()).isEqualTo("Workspace invitation");
        assertThat(notification.getMetadata()).containsKey("workspaceId");
    }

    @Test
    void createWorkspaceNotification_inviteDeclinedEvent_buildsDeclinedNotification() {
        WorkspaceNotificationEvent event = workspaceEvent("WORKSPACE_INVITE_DECLINED");

        Notification notification = notificationFactory.createWorkspaceNotification(event);

        assertThat(notification.getType()).isEqualTo(NotificationType.WORKSPACE_INVITE_DECLINED);
        assertThat(notification.getBody()).contains("Kate declined the invite");
        assertThat(notification.getMetadata()).containsEntry("subjectDisplayName", "Kate");
    }

    @Test
    void createCommentNotification_commentEvent_includesNavigationMetadata() {
        CommentNotificationEvent event = commentEvent("COMMENT_CREATED");

        Notification notification = notificationFactory.createCommentNotification(event);

        assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_CREATED);
        assertThat(notification.getMetadata())
                .containsEntry("workspaceId", event.workspaceId())
                .containsEntry("boardId", event.boardId())
                .containsEntry("taskId", event.taskId())
                .containsEntry("commentId", event.commentId());
    }

    @Test
    void createTaskNotification_unknownEvent_returnsNull() {
        assertThat(notificationFactory.createTaskNotification(taskEvent("UNKNOWN"))).isNull();
    }
}
