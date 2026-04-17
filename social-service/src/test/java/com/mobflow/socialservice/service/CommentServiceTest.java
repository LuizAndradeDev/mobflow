package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.client.WorkspaceServiceClient;
import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.dto.request.CreateCommentRequest;
import com.mobflow.socialservice.model.dto.request.UpdateCommentRequest;
import com.mobflow.socialservice.model.dto.response.CommentResponse;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.model.enums.WorkspaceRole;
import com.mobflow.socialservice.repository.CommentRepository;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.CommentTestFixtures.AUTHOR_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.WORKSPACE_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.authenticatedUser;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.comment;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.createCommentRequest;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.pageOf;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.taskContext;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.updateCommentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskServiceClient taskServiceClient;

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    @Mock
    private MentionService mentionService;

    @Mock
    private CommentFactory commentFactory;

    @Mock
    private SocialNotificationService socialNotificationService;

    @InjectMocks
    private CommentService commentService;

    @Captor
    private ArgumentCaptor<Comment> commentCaptor;

    private AuthenticatedUser author;

    @BeforeEach
    void setUp() {
        author = authenticatedUser();
    }

    @Test
    void createComment_validRequest_returnsCreatedComment() {
        CreateCommentRequest request = createCommentRequest("Hello team");
        Comment unsavedComment = comment();
        Comment savedComment = comment();
        savedComment.setId(UUID.randomUUID());

        when(taskServiceClient.getTaskContext(TASK_ID)).thenReturn(taskContext());
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(mentionService.resolveMentions("Hello team", WORKSPACE_ID)).thenReturn(List.of());
        when(commentFactory.create(taskContext(), author, "Hello team", List.of())).thenReturn(unsavedComment);
        when(commentRepository.save(unsavedComment)).thenReturn(savedComment);

        CommentResponse response = commentService.createComment(TASK_ID, author, request);

        assertThat(response.id()).isEqualTo(savedComment.getId());
        assertThat(response.content()).isEqualTo(savedComment.getContent());
        verify(socialNotificationService).publishCommentCreated(savedComment, taskContext(), author);
        verify(socialNotificationService).publishMentions(savedComment, taskContext(), author, List.of());
    }

    @Test
    void createComment_validMentions_callsNotificationWithResolvedMentions() {
        CreateCommentRequest request = createCommentRequest("Hello @mary_dev");
        List<MentionService.ResolvedMention> mentions = List.of(
                new MentionService.ResolvedMention(UUID.randomUUID(), "mary_dev")
        );
        Comment unsavedComment = comment();
        Comment savedComment = comment();

        when(taskServiceClient.getTaskContext(TASK_ID)).thenReturn(taskContext());
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(mentionService.resolveMentions("Hello @mary_dev", WORKSPACE_ID)).thenReturn(mentions);
        when(commentFactory.create(taskContext(), author, "Hello @mary_dev", List.of("mary_dev"))).thenReturn(unsavedComment);
        when(commentRepository.save(unsavedComment)).thenReturn(savedComment);

        commentService.createComment(TASK_ID, author, request);

        verify(commentFactory).create(taskContext(), author, "Hello @mary_dev", List.of("mary_dev"));
        verify(socialNotificationService).publishMentions(savedComment, taskContext(), author, mentions);
    }

    @Test
    void createComment_invalidMentionsIgnored_usesOnlyResolvedMentions() {
        CreateCommentRequest request = createCommentRequest("Hello @mary_dev and @ghost_user");
        List<MentionService.ResolvedMention> mentions = List.of(
                new MentionService.ResolvedMention(UUID.randomUUID(), "mary_dev")
        );
        Comment unsavedComment = comment();

        when(taskServiceClient.getTaskContext(TASK_ID)).thenReturn(taskContext());
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(mentionService.resolveMentions("Hello @mary_dev and @ghost_user", WORKSPACE_ID)).thenReturn(mentions);
        when(commentFactory.create(taskContext(), author, "Hello @mary_dev and @ghost_user", List.of("mary_dev"))).thenReturn(unsavedComment);
        when(commentRepository.save(unsavedComment)).thenReturn(unsavedComment);

        commentService.createComment(TASK_ID, author, request);

        verify(commentFactory).create(taskContext(), author, "Hello @mary_dev and @ghost_user", List.of("mary_dev"));
    }

    @Test
    void updateComment_authorEditsComment_returnsUpdatedComment() {
        UUID commentId = UUID.randomUUID();
        UpdateCommentRequest request = updateCommentRequest("Updated content");
        Comment existingComment = comment(commentId, TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "Old", List.of(), false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(mentionService.resolveMentions("Updated content", WORKSPACE_ID)).thenReturn(List.of());
        when(commentRepository.save(existingComment)).thenReturn(existingComment);

        CommentResponse response = commentService.updateComment(commentId, author, request);

        assertThat(response.content()).isEqualTo("Updated content");
        assertThat(existingComment.getEditedAt()).isNotNull();
    }

    @Test
    void updateComment_nonAuthor_throwsAccessDenied() {
        UUID commentId = UUID.randomUUID();
        AuthenticatedUser anotherUser = authenticatedUser(UUID.randomUUID(), "another_user");
        Comment existingComment = comment(commentId, TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "Old", List.of(), false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, anotherUser.authId())).thenReturn(WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> commentService.updateComment(commentId, anotherUser, updateCommentRequest("Updated content")))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("You are not allowed to perform this action");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void deleteComment_authorSoftDeletesComment_persistsDeletedState() {
        UUID commentId = UUID.randomUUID();
        Comment existingComment = comment(commentId, TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "Old", List.of("mary_dev"), false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(commentRepository.save(existingComment)).thenReturn(existingComment);

        commentService.deleteComment(commentId, author);

        verify(commentRepository).save(existingComment);
        assertThat(existingComment.isDeleted()).isTrue();
        assertThat(existingComment.getContent()).isEmpty();
        assertThat(existingComment.getMentions()).isEmpty();
    }

    @Test
    void deleteComment_adminSoftDeletesAnotherUsersComment_persistsDeletedState() {
        UUID commentId = UUID.randomUUID();
        AuthenticatedUser admin = authenticatedUser(UUID.randomUUID(), "admin_user");
        Comment existingComment = comment(commentId, TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "other_user", "Old", List.of(), false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, admin.authId())).thenReturn(WorkspaceRole.ADMIN);
        when(commentRepository.save(existingComment)).thenReturn(existingComment);

        commentService.deleteComment(commentId, admin);

        verify(commentRepository).save(existingComment);
        assertThat(existingComment.isDeleted()).isTrue();
    }

    @Test
    void deleteComment_userWithoutPermission_throwsAccessDenied() {
        UUID commentId = UUID.randomUUID();
        AuthenticatedUser member = authenticatedUser(UUID.randomUUID(), "member_user");
        Comment existingComment = comment(commentId, TASK_ID, WORKSPACE_ID, UUID.randomUUID(), "other_user", "Old", List.of(), false);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, member.authId())).thenReturn(WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> commentService.deleteComment(commentId, member))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("You are not allowed to perform this action");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void listComments_validRequest_returnsPaginatedComments() {
        PageRequest pageable = PageRequest.of(0, 20);
        Comment first = comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "One", List.of(), false);
        Comment second = comment(UUID.randomUUID(), TASK_ID, WORKSPACE_ID, AUTHOR_ID, "author_user", "Two", List.of(), false);

        when(taskServiceClient.getTaskContext(TASK_ID)).thenReturn(taskContext());
        when(workspaceServiceClient.requireMembership(WORKSPACE_ID, AUTHOR_ID)).thenReturn(WorkspaceRole.MEMBER);
        when(commentRepository.findByTaskId(eq(TASK_ID), any())).thenReturn(pageOf(pageable, first, second));

        Page<CommentResponse> response = commentService.listComments(TASK_ID, author, 0, 20);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).extracting(CommentResponse::content).containsExactly("One", "Two");
    }

    @Test
    void updateComment_commentNotFound_throwsNotFound() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(commentId, author, updateCommentRequest("Updated content")))
                .isInstanceOf(SocialServiceException.class)
                .hasMessage("Comment not found");
    }
}
