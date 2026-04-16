package com.mobflow.socialservice.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "comments")
@CompoundIndex(name = "idx_comment_task_created", def = "{'taskId': 1, 'createdAt': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Comment {

    @Id
    private UUID id;
    private UUID taskId;
    private UUID workspaceId;
    private UUID authorId;
    private String authorUsername;
    private String content;
    private List<String> mentions;
    @CreatedDate
    private Instant createdAt;
    private Instant editedAt;
    private boolean deleted;

    public static Comment create(
            UUID taskId,
            UUID workspaceId,
            UUID authorId,
            String authorUsername,
            String content,
            List<String> mentions
    ) {
        return Comment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .workspaceId(workspaceId)
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(content)
                .mentions(new ArrayList<>(mentions))
                .deleted(false)
                .build();
    }

    public void edit(String newContent, List<String> newMentions) {
        this.content = newContent;
        this.mentions = new ArrayList<>(newMentions);
        this.editedAt = Instant.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.content = "";
        this.mentions = new ArrayList<>();
    }
}
