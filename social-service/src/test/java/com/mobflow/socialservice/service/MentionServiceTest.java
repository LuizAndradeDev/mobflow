package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.client.WorkspaceServiceClient;
import com.mobflow.socialservice.exception.SocialServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    @InjectMocks
    private MentionService mentionService;

    @Test
    void resolveMentions_membersInsideWorkspace_returnResolvedMentions() {
        UUID workspaceId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();
        UUID carlosId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev", "ghost_user", "carlos123")))
                .thenReturn(Map.of(
                        "ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev"),
                        "carlos123", new AuthServiceClient.AuthUserSummaryResponse(carlosId, "carlos123")
                ));
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, anaId)).thenReturn(true);
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, carlosId)).thenReturn(true);

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "Hello @ana_dev, meet @ghost_user and @carlos123. Duplicate @ana_dev should not duplicate.",
                workspaceId
        );

        assertThat(mentions)
                .containsExactly(
                        new MentionService.ResolvedMention(anaId, "ana_dev"),
                        new MentionService.ResolvedMention(carlosId, "carlos123")
                );
    }

    @Test
    void resolveMentions_userOutsideWorkspace_isIgnored() {
        UUID workspaceId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();
        UUID carlosId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev", "carlos123")))
                .thenReturn(Map.of(
                        "ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev"),
                        "carlos123", new AuthServiceClient.AuthUserSummaryResponse(carlosId, "carlos123")
                ));
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, anaId)).thenReturn(true);
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, carlosId)).thenReturn(false);

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "@ana_dev talked to @carlos123 and again to @ana_dev",
                workspaceId
        );

        assertThat(mentions)
                .containsExactly(new MentionService.ResolvedMention(anaId, "ana_dev"));
    }

    @Test
    void resolveMentions_invalidPatterns_returnsEmptyList() {
        UUID workspaceId = UUID.randomUUID();

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "@ab invalid @@double invalid @name-with-dash invalid",
                workspaceId
        );

        assertThat(mentions).isEmpty();
    }

    @Test
    void resolveMentions_emptyOrNullContent_returnsEmptyList() {
        UUID workspaceId = UUID.randomUUID();

        assertThat(mentionService.resolveMentions("   ", workspaceId)).isEmpty();
        assertThat(mentionService.resolveMentions(null, workspaceId)).isEmpty();
    }

    @Test
    void resolveMentions_mixedWorkspaceMembership_onlyValidMembersRemain() {
        UUID workspaceId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();
        UUID maryId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev", "mary_dev", "ghost_user")))
                .thenReturn(Map.of(
                        "ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev"),
                        "mary_dev", new AuthServiceClient.AuthUserSummaryResponse(maryId, "mary_dev")
                ));
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, anaId)).thenReturn(true);
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, maryId)).thenReturn(false);

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "@ana_dev @mary_dev @ghost_user",
                workspaceId
        );

        assertThat(mentions).containsExactly(new MentionService.ResolvedMention(anaId, "ana_dev"));
    }

    @Test
    void resolveMentions_whenWorkspaceMembershipCheckIsUnavailable_skipsMentions() {
        UUID workspaceId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev")))
                .thenReturn(Map.of("ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev")));
        when(workspaceServiceClient.isWorkspaceMember(workspaceId, anaId))
                .thenThrow(SocialServiceException.upstreamServiceError());

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions("@ana_dev", workspaceId);

        assertThat(mentions).isEmpty();
    }
}
