package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.kafka.events.CommentNotificationEvent;
import com.mobflow.socialservice.kafka.events.FriendRequestEvent;
import com.mobflow.socialservice.kafka.producers.SocialEventProducer;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ASSIGNEE_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.BOARD_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_CREATOR_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.authenticatedUser;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.comment;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.taskContext;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.friendRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialNotificationServiceTest {

    @Mock
    private SocialEventProducer socialEventProducer;

    @InjectMocks
    private SocialNotificationService socialNotificationService;

    @Captor
    private ArgumentCaptor<CommentNotificationEvent> commentEventCaptor;

    @Captor
    private ArgumentCaptor<FriendRequestEvent> friendRequestEventCaptor;

    @Test
    void publishCommentCreated_distinctRecipients_emitsOneEventPerRecipientExceptActor() {
        Comment comment = comment();
        AuthenticatedUser actor = authenticatedUser(UUID.randomUUID(), "actor_user");
        TaskServiceClient.TaskCommentContextResponse taskContext = new TaskServiceClient.TaskCommentContextResponse(
                taskContext().taskId(),
                taskContext().workspaceId(),
                BOARD_ID,
                TASK_CREATOR_ID,
                TASK_ASSIGNEE_ID,
                taskContext().taskTitle()
        );

        socialNotificationService.publishCommentCreated(comment, taskContext, actor);

        verify(socialEventProducer, times(2)).publishCommentEvent(commentEventCaptor.capture());
        assertThat(commentEventCaptor.getAllValues())
                .extracting(CommentNotificationEvent::eventType, CommentNotificationEvent::recipientId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("COMMENT_CREATED", TASK_CREATOR_ID.toString()),
                        org.assertj.core.groups.Tuple.tuple("COMMENT_CREATED", TASK_ASSIGNEE_ID.toString())
                );
    }

    @Test
    void publishMentions_actorMentionIgnored_emitsOnlyExternalMentionEvents() {
        Comment comment = comment();
        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = authenticatedUser(actorId, "actor_user");

        socialNotificationService.publishMentions(
                comment,
                taskContext(),
                actor,
                List.of(
                        new MentionService.ResolvedMention(actorId, "actor_user"),
                        new MentionService.ResolvedMention(UUID.randomUUID(), "mary_dev")
                )
        );

        verify(socialEventProducer).publishCommentEvent(commentEventCaptor.capture());
        assertThat(commentEventCaptor.getValue().eventType()).isEqualTo("USER_MENTIONED");
        assertThat(commentEventCaptor.getValue().mentionedUsername()).isEqualTo("mary_dev");
    }

    @Test
    void publishFriendRequestAccepted_validRequest_emitsFriendshipEvent() {
        FriendRequest request = friendRequest();
        AuthenticatedUser actor = authenticatedUser(UUID.randomUUID(), "mary_dev");

        socialNotificationService.publishFriendRequestAccepted(request, actor);

        verify(socialEventProducer).publishFriendRequestEvent(friendRequestEventCaptor.capture());
        assertThat(friendRequestEventCaptor.getValue().eventType()).isEqualTo("FRIEND_REQUEST_ACCEPTED");
        assertThat(friendRequestEventCaptor.getValue().recipientId()).isEqualTo(request.getRequesterId().toString());
    }
}
