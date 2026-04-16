package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.kafka.events.CommentNotificationEvent;
import com.mobflow.socialservice.kafka.events.FriendRequestEvent;
import com.mobflow.socialservice.kafka.producers.SocialEventProducer;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SocialNotificationService {

    private static final int COMMENT_PREVIEW_LIMIT = 140;

    private final SocialEventProducer socialEventProducer;

    public SocialNotificationService(SocialEventProducer socialEventProducer) {
        this.socialEventProducer = socialEventProducer;
    }

    public void publishCommentCreated(
            Comment comment,
            TaskServiceClient.TaskCommentContextResponse taskContext,
            AuthenticatedUser actor
    ) {
        Set<UUID> recipients = new LinkedHashSet<>();
        if (taskContext.createdByAuthId() != null) {
            recipients.add(taskContext.createdByAuthId());
        }
        if (taskContext.assigneeAuthId() != null) {
            recipients.add(taskContext.assigneeAuthId());
        }

        for (UUID recipientId : recipients) {
            if (recipientId.equals(actor.authId())) {
                continue;
            }

            socialEventProducer.publishCommentEvent(new CommentNotificationEvent(
                    "COMMENT_CREATED",
                    recipientId.toString(),
                    actor.authId().toString(),
                    actor.username(),
                    taskContext.taskId().toString(),
                    taskContext.workspaceId().toString(),
                    comment.getId().toString(),
                    taskContext.taskTitle(),
                    buildCommentPreview(comment.getContent()),
                    null,
                    Instant.now()
            ));
        }
    }

    public void publishMentions(
            Comment comment,
            TaskServiceClient.TaskCommentContextResponse taskContext,
            AuthenticatedUser actor,
            List<MentionService.ResolvedMention> mentions
    ) {
        for (MentionService.ResolvedMention mention : mentions) {
            if (mention.authId().equals(actor.authId())) {
                continue;
            }

            socialEventProducer.publishCommentEvent(new CommentNotificationEvent(
                    "USER_MENTIONED",
                    mention.authId().toString(),
                    actor.authId().toString(),
                    actor.username(),
                    taskContext.taskId().toString(),
                    taskContext.workspaceId().toString(),
                    comment.getId().toString(),
                    taskContext.taskTitle(),
                    buildCommentPreview(comment.getContent()),
                    mention.username(),
                    Instant.now()
            ));
        }
    }

    public void publishFriendRequestSent(FriendRequest friendRequest, AuthenticatedUser actor) {
        socialEventProducer.publishFriendRequestEvent(new FriendRequestEvent(
                "FRIEND_REQUEST_SENT",
                friendRequest.getTargetId().toString(),
                actor.authId().toString(),
                actor.username(),
                friendRequest.getId().toString(),
                friendRequest.getTargetId().toString(),
                friendRequest.getTargetUsername(),
                Instant.now()
        ));
    }

    public void publishFriendRequestAccepted(FriendRequest friendRequest, AuthenticatedUser actor) {
        socialEventProducer.publishFriendRequestEvent(new FriendRequestEvent(
                "FRIEND_REQUEST_ACCEPTED",
                friendRequest.getRequesterId().toString(),
                actor.authId().toString(),
                actor.username(),
                friendRequest.getId().toString(),
                friendRequest.getRequesterId().toString(),
                friendRequest.getRequesterUsername(),
                Instant.now()
        ));
    }

    private String buildCommentPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= COMMENT_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, COMMENT_PREVIEW_LIMIT - 3) + "...";
    }
}
