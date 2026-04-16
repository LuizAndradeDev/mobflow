package com.mobflow.socialservice.model.dto.response;

import com.mobflow.socialservice.model.entities.Comment;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID workspaceId,
        UUID authorId,
        String authorUsername,
        String content,
        List<String> mentions,
        Instant createdAt,
        Instant editedAt,
        boolean deleted
) {
    public static CommentResponse fromEntity(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .workspaceId(comment.getWorkspaceId())
                .authorId(comment.getAuthorId())
                .authorUsername(comment.getAuthorUsername())
                .content(comment.isDeleted() ? null : comment.getContent())
                .mentions(comment.isDeleted() ? List.of() : List.copyOf(comment.getMentions()))
                .createdAt(comment.getCreatedAt())
                .editedAt(comment.getEditedAt())
                .deleted(comment.isDeleted())
                .build();
    }
}
