package com.mobflow.socialservice.testsupport;

import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.model.dto.request.CreateCommentRequest;
import com.mobflow.socialservice.model.dto.request.UpdateCommentRequest;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommentTestFixtures {

    public static final UUID TASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID WORKSPACE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID BOARD_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
    public static final UUID AUTHOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID TASK_CREATOR_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID TASK_ASSIGNEE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    private CommentTestFixtures() {
    }

    public static AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTHOR_ID, "author_user");
    }

    public static AuthenticatedUser authenticatedUser(UUID authId, String username) {
        return new AuthenticatedUser(authId, username);
    }

    public static CreateCommentRequest createCommentRequest(String content) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent(content);
        return request;
    }

    public static UpdateCommentRequest updateCommentRequest(String content) {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent(content);
        return request;
    }

    public static Comment comment() {
        return comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "First comment", List.of("mary"), false);
    }

    public static Comment comment(
            UUID id,
            UUID taskId,
            UUID workspaceId,
            UUID authorId,
            String authorUsername,
            String content,
            List<String> mentions,
            boolean deleted
    ) {
        Comment comment = Comment.builder()
                .id(id)
                .taskId(taskId)
                .workspaceId(workspaceId)
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(content)
                .mentions(mentions)
                .createdAt(Instant.now())
                .editedAt(null)
                .deleted(deleted)
                .build();
        return comment;
    }

    public static TaskServiceClient.TaskCommentContextResponse taskContext() {
        return new TaskServiceClient.TaskCommentContextResponse(
                TASK_ID,
                WORKSPACE_ID,
                BOARD_ID,
                TASK_CREATOR_ID,
                TASK_ASSIGNEE_ID,
                "Prepare roadmap"
        );
    }

    public static Page<Comment> pageOf(Pageable pageable, Comment... comments) {
        return new PageImpl<>(List.of(comments), pageable, comments.length);
    }
}
