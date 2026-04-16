package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentFactory {

    public Comment create(
            TaskServiceClient.TaskCommentContextResponse taskContext,
            AuthenticatedUser authenticatedUser,
            String content,
            List<String> mentions
    ) {
        return Comment.create(
                taskContext.taskId(),
                taskContext.workspaceId(),
                authenticatedUser.authId(),
                authenticatedUser.username(),
                content,
                mentions
        );
    }
}
