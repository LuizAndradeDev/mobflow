package com.mobflow.notificationservice.service;

import com.mobflow.notificationservice.kafka.events.AuthNotificationEvent;
import com.mobflow.notificationservice.kafka.events.ChatMessageNotificationEvent;
import com.mobflow.notificationservice.kafka.events.CommentNotificationEvent;
import com.mobflow.notificationservice.kafka.events.FriendRequestEvent;
import com.mobflow.notificationservice.kafka.events.TaskNotificationEvent;
import com.mobflow.notificationservice.kafka.events.WorkspaceNotificationEvent;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.model.enums.NotificationChannel;
import com.mobflow.notificationservice.model.enums.NotificationPriority;
import com.mobflow.notificationservice.model.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationFactory {

    public Notification createAuthNotification(AuthNotificationEvent event) {
        return Notification.builder()
                .recipientId(event.recipientId())
                .recipientEmail(event.recipientEmail())
                .title("Confirm your Mobflow account")
                .body("Finish your registration by confirming your email address.")
                .type(NotificationType.EMAIL_CONFIRMATION)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .metadata(Map.of(
                        "confirmationToken", safe(event.confirmationToken()),
                        "confirmationUrl", safe(event.confirmationUrl()),
                        "recipientUsername", safe(event.recipientUsername())
                ))
                .build();
    }

    public Notification createTaskNotification(TaskNotificationEvent event) {
        if (event.recipientId() == null || event.recipientId().isBlank()) {
            return null;
        }

        NotificationType type = switch (event.eventType()) {
            case "TASK_CREATED" -> NotificationType.TASK_CREATED;
            case "TASK_ASSIGNED" -> NotificationType.TASK_ASSIGNED;
            case "TASK_UPDATED" -> NotificationType.TASK_UPDATED;
            case "TASK_DELETED" -> NotificationType.TASK_DELETED;
            case "TASK_COMPLETED" -> NotificationType.TASK_COMPLETED;
            case "TASK_DUE_SOON" -> NotificationType.TASK_DUE_SOON;
            default -> null;
        };

        if (type == null) {
            return null;
        }

        return Notification.builder()
                .recipientId(event.recipientId())
                .recipientEmail(event.recipientEmail())
                .title(taskTitleFor(type, event))
                .body(taskBodyFor(type, event))
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .priority(type == NotificationType.TASK_DUE_SOON ? NotificationPriority.HIGH : NotificationPriority.MEDIUM)
                .metadata(taskMetadata(event))
                .build();
    }

    public Notification createWorkspaceNotification(WorkspaceNotificationEvent event) {
        if (event.recipientId() == null || event.recipientId().isBlank()) {
            return null;
        }

        NotificationType type = switch (event.eventType()) {
            case "WORKSPACE_INVITE" -> NotificationType.WORKSPACE_INVITE;
            case "WORKSPACE_INVITE_ACCEPTED" -> NotificationType.WORKSPACE_INVITE_ACCEPTED;
            case "WORKSPACE_INVITE_DECLINED" -> NotificationType.WORKSPACE_INVITE_DECLINED;
            case "WORKSPACE_MEMBER_ADDED" -> NotificationType.WORKSPACE_MEMBER_ADDED;
            case "WORKSPACE_MEMBER_REMOVED" -> NotificationType.WORKSPACE_MEMBER_REMOVED;
            case "WORKSPACE_ROLE_CHANGED" -> NotificationType.WORKSPACE_ROLE_CHANGED;
            default -> null;
        };

        if (type == null) {
            return null;
        }

        return Notification.builder()
                .recipientId(event.recipientId())
                .recipientEmail(event.recipientEmail())
                .title(workspaceTitleFor(type, event))
                .body(workspaceBodyFor(type, event))
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.MEDIUM)
                .metadata(workspaceMetadata(event))
                .build();
    }

    public Notification createCommentNotification(CommentNotificationEvent event) {
        if (event.recipientId() == null || event.recipientId().isBlank()) {
            return null;
        }

        NotificationType type = switch (event.eventType()) {
            case "COMMENT_CREATED" -> NotificationType.COMMENT_CREATED;
            case "USER_MENTIONED" -> NotificationType.USER_MENTIONED;
            default -> null;
        };

        if (type == null) {
            return null;
        }

        return Notification.builder()
                .recipientId(event.recipientId())
                .title(commentTitleFor(type, event))
                .body(commentBodyFor(type, event))
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .priority(type == NotificationType.USER_MENTIONED ? NotificationPriority.HIGH : NotificationPriority.MEDIUM)
                .metadata(commentMetadata(event))
                .build();
    }

    public Notification createChatMessageNotification(ChatMessageNotificationEvent event) {
        if (event.recipientId() == null || event.recipientId().isBlank()) {
            return null;
        }

        if (!"NEW_CHAT_MESSAGE".equals(event.eventType())) {
            return null;
        }

        return Notification.builder()
                .recipientId(event.recipientId())
                .title("New chat message")
                .body(chatMessageBodyFor(event))
                .type(NotificationType.CHAT_MESSAGE_RECEIVED)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.MEDIUM)
                .metadata(chatMessageMetadata(event))
                .build();
    }

    public Notification createFriendRequestNotification(FriendRequestEvent event) {
        if (event.recipientId() == null || event.recipientId().isBlank()) {
            return null;
        }

        NotificationType type = switch (event.eventType()) {
            case "FRIEND_REQUEST_SENT" -> NotificationType.FRIEND_REQUEST_SENT;
            case "FRIEND_REQUEST_ACCEPTED" -> NotificationType.FRIEND_REQUEST_ACCEPTED;
            default -> null;
        };

        if (type == null) {
            return null;
        }

        return Notification.builder()
                .recipientId(event.recipientId())
                .title(friendRequestTitleFor(type))
                .body(friendRequestBodyFor(type, event))
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.MEDIUM)
                .metadata(friendRequestMetadata(event))
                .build();
    }

    private String taskTitleFor(NotificationType type, TaskNotificationEvent event) {
        return switch (type) {
            case TASK_CREATED -> "Task created: " + safe(event.taskTitle());
            case TASK_ASSIGNED -> "New task assigned";
            case TASK_UPDATED -> "Task updated: " + safe(event.taskTitle());
            case TASK_DELETED -> "Task removed";
            case TASK_COMPLETED -> "Task completed";
            case TASK_DUE_SOON -> "Task due soon";
            default -> "Task notification";
        };
    }

    private String taskBodyFor(NotificationType type, TaskNotificationEvent event) {
        String actor = blankFallback(event.actorDisplayName(), "A workspace member");
        return switch (type) {
            case TASK_CREATED -> actor + " created \"" + safe(event.taskTitle()) + "\" in " + safe(event.workspaceName()) + ".";
            case TASK_ASSIGNED -> actor + " assigned \"" + safe(event.taskTitle()) + "\" to you.";
            case TASK_UPDATED -> actor + " updated \"" + safe(event.taskTitle()) + "\".";
            case TASK_DELETED -> actor + " deleted \"" + safe(event.taskTitle()) + "\".";
            case TASK_COMPLETED -> actor + " completed \"" + safe(event.taskTitle()) + "\".";
            case TASK_DUE_SOON -> "\"" + safe(event.taskTitle()) + "\" is due on " + safe(String.valueOf(event.dueDate())) + ".";
            default -> "Task update available.";
        };
    }

    private String workspaceTitleFor(NotificationType type, WorkspaceNotificationEvent event) {
        return switch (type) {
            case WORKSPACE_INVITE -> "Workspace invitation";
            case WORKSPACE_INVITE_ACCEPTED -> "Invitation accepted";
            case WORKSPACE_INVITE_DECLINED -> "Invitation declined";
            case WORKSPACE_MEMBER_ADDED -> "Member added";
            case WORKSPACE_MEMBER_REMOVED -> "Member removed";
            case WORKSPACE_ROLE_CHANGED -> "Role updated";
            default -> "Workspace update";
        };
    }

    private String workspaceBodyFor(NotificationType type, WorkspaceNotificationEvent event) {
        String actor = blankFallback(event.actorDisplayName(), "A workspace admin");
        String workspaceName = blankFallback(event.workspaceName(), "your workspace");
        String subject = blankFallback(event.subjectDisplayName(), "A user");
        return switch (type) {
            case WORKSPACE_INVITE -> actor + " invited you to join " + workspaceName + ".";
            case WORKSPACE_INVITE_ACCEPTED -> subject + " accepted the invite to " + workspaceName + ".";
            case WORKSPACE_INVITE_DECLINED -> subject + " declined the invite to " + workspaceName + ".";
            case WORKSPACE_MEMBER_ADDED -> subject + " joined " + workspaceName + ".";
            case WORKSPACE_MEMBER_REMOVED -> actor + " removed " + subject + " from " + workspaceName + ".";
            case WORKSPACE_ROLE_CHANGED -> actor + " changed a workspace role to " + blankFallback(event.role(), "the new role") + ".";
            default -> "Workspace update available.";
        };
    }

    private String commentTitleFor(NotificationType type, CommentNotificationEvent event) {
        return switch (type) {
            case COMMENT_CREATED -> "New comment on task";
            case USER_MENTIONED -> "You were mentioned in a comment";
            default -> "Comment notification";
        };
    }

    private String commentBodyFor(NotificationType type, CommentNotificationEvent event) {
        String actor = blankFallback(event.actorUsername(), "A workspace member");
        return switch (type) {
            case COMMENT_CREATED -> actor + " commented on \"" + safe(event.taskTitle()) + "\".";
            case USER_MENTIONED -> actor + " mentioned you in a comment on \"" + safe(event.taskTitle()) + "\".";
            default -> "Comment update available.";
        };
    }

    private String chatMessageBodyFor(ChatMessageNotificationEvent event) {
        String contentPreview = safe(event.contentPreview()).trim();
        if (contentPreview.isBlank()) {
            return "You received a new message.";
        }
        return contentPreview;
    }

    private String friendRequestTitleFor(NotificationType type) {
        return switch (type) {
            case FRIEND_REQUEST_SENT -> "New friend request";
            case FRIEND_REQUEST_ACCEPTED -> "Friend request accepted";
            default -> "Friendship update";
        };
    }

    private String friendRequestBodyFor(NotificationType type, FriendRequestEvent event) {
        String actor = blankFallback(event.actorUsername(), "A Mobflow user");
        return switch (type) {
            case FRIEND_REQUEST_SENT -> actor + " sent you a friend request.";
            case FRIEND_REQUEST_ACCEPTED -> actor + " accepted your friend request.";
            default -> "Friendship update available.";
        };
    }

    private Map<String, String> taskMetadata(TaskNotificationEvent event) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("taskId", safe(event.taskId()));
        metadata.put("taskTitle", safe(event.taskTitle()));
        metadata.put("workspaceId", safe(event.workspaceId()));
        metadata.put("boardId", safe(event.boardId()));
        metadata.put("workspaceName", safe(event.workspaceName()));
        metadata.put("taskStatus", safe(event.taskStatus()));
        metadata.put("dueDate", safe(String.valueOf(event.dueDate())));
        metadata.put("actorAuthId", safe(event.actorAuthId()));
        metadata.put("actorDisplayName", safe(event.actorDisplayName()));
        return metadata;
    }

    private Map<String, String> workspaceMetadata(WorkspaceNotificationEvent event) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("workspaceId", safe(event.workspaceId()));
        metadata.put("workspaceName", safe(event.workspaceName()));
        metadata.put("inviteId", safe(event.inviteId()));
        metadata.put("actorAuthId", safe(event.actorAuthId()));
        metadata.put("actorDisplayName", safe(event.actorDisplayName()));
        metadata.put("subjectAuthId", safe(event.subjectAuthId()));
        metadata.put("subjectDisplayName", safe(event.subjectDisplayName()));
        metadata.put("role", safe(event.role()));
        return metadata;
    }

    private Map<String, String> commentMetadata(CommentNotificationEvent event) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("taskId", safe(event.taskId()));
        metadata.put("workspaceId", safe(event.workspaceId()));
        metadata.put("boardId", safe(event.boardId()));
        metadata.put("commentId", safe(event.commentId()));
        metadata.put("taskTitle", safe(event.taskTitle()));
        metadata.put("commentPreview", safe(event.commentPreview()));
        metadata.put("actorAuthId", safe(event.actorAuthId()));
        metadata.put("actorUsername", safe(event.actorUsername()));
        metadata.put("mentionedUsername", safe(event.mentionedUsername()));
        return metadata;
    }

    private Map<String, String> friendRequestMetadata(FriendRequestEvent event) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("requestId", safe(event.requestId()));
        metadata.put("actorAuthId", safe(event.actorAuthId()));
        metadata.put("actorUsername", safe(event.actorUsername()));
        metadata.put("subjectAuthId", safe(event.subjectAuthId()));
        metadata.put("subjectUsername", safe(event.subjectUsername()));
        return metadata;
    }

    private Map<String, String> chatMessageMetadata(ChatMessageNotificationEvent event) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("messageId", safe(event.messageId()));
        metadata.put("conversationId", safe(event.conversationId()));
        metadata.put("senderId", safe(event.senderId()));
        metadata.put("contentPreview", safe(event.contentPreview()));
        return metadata;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
