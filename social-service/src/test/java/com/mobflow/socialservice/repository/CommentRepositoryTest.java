package com.mobflow.socialservice.repository;

import com.mobflow.socialservice.config.MongoConfig;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.testsupport.AbstractMongoSocialTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.WORKSPACE_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.comment;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
class CommentRepositoryTest extends AbstractMongoSocialTest {

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void cleanUp() {
        commentRepository.deleteAll();
    }

    @Test
    void save_validComment_persistsDocument() {
        Comment savedComment = commentRepository.save(comment());

        assertThat(savedComment.getId()).isNotNull();
        assertThat(commentRepository.findById(savedComment.getId())).isPresent();
    }

    @Test
    void findByTaskId_paginatedRequest_returnsCommentsSortedByCreatedAt() {
        UUID anotherTaskId = UUID.randomUUID();
        Comment first = comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "john_dev", "First", java.util.List.of(), false);
        first.setCreatedAt(Instant.parse("2025-01-01T10:00:00Z"));
        Comment second = comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "mary_dev", "Second", java.util.List.of(), false);
        second.setCreatedAt(Instant.parse("2025-01-01T11:00:00Z"));
        Comment third = comment(UUID.randomUUID(), anotherTaskId, WORKSPACE_ID, UUID.randomUUID(), "peter_dev", "Ignored", java.util.List.of(), false);
        third.setCreatedAt(Instant.parse("2025-01-01T12:00:00Z"));

        commentRepository.save(first);
        commentRepository.save(second);
        commentRepository.save(third);

        Page<Comment> page = commentRepository.findByTaskId(
                TASK_ID,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        assertThat(page.getContent()).extracting(Comment::getContent).containsExactly("First", "Second");
    }

    @Test
    void findByTaskId_softDeletedComment_includesDeletedRecord() {
        Comment deletedComment = comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "john_dev", "", java.util.List.of(), true);
        deletedComment.setCreatedAt(Instant.parse("2025-01-01T10:00:00Z"));
        commentRepository.save(deletedComment);

        Page<Comment> page = commentRepository.findByTaskId(TASK_ID, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().extracting(Comment::isDeleted).isEqualTo(true);
    }
}
