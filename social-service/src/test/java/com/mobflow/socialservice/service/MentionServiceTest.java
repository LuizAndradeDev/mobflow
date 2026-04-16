package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
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

    @InjectMocks
    private MentionService mentionService;

    @Test
    void shouldResolveValidMentionsAndIgnoreInvalidUsernames() {
        UUID anaId = UUID.randomUUID();
        UUID carlosId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev", "ghost_user", "carlos123")))
                .thenReturn(Map.of(
                        "ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev"),
                        "carlos123", new AuthServiceClient.AuthUserSummaryResponse(carlosId, "carlos123")
                ));

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "Hello @ana_dev, meet @ghost_user and @carlos123. Duplicate @ana_dev should not duplicate."
        );

        assertThat(mentions)
                .containsExactly(
                        new MentionService.ResolvedMention(anaId, "ana_dev"),
                        new MentionService.ResolvedMention(carlosId, "carlos123")
                );
    }

    @Test
    void resolveMentions_duplicateMentions_returnsUniqueMentionsInOriginalOrder() {
        UUID anaId = UUID.randomUUID();

        when(authServiceClient.resolveByUsernames(List.of("ana_dev", "carlos123")))
                .thenReturn(Map.of(
                        "ana_dev", new AuthServiceClient.AuthUserSummaryResponse(anaId, "ana_dev")
                ));

        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "@ana_dev talked to @carlos123 and again to @ana_dev"
        );

        assertThat(mentions)
                .containsExactly(new MentionService.ResolvedMention(anaId, "ana_dev"));
    }

    @Test
    void resolveMentions_invalidPatterns_returnsEmptyList() {
        List<MentionService.ResolvedMention> mentions = mentionService.resolveMentions(
                "@ab invalid @@double invalid @name-with-dash invalid"
        );

        assertThat(mentions).isEmpty();
    }

    @Test
    void resolveMentions_emptyOrNullContent_returnsEmptyList() {
        assertThat(mentionService.resolveMentions("   ")).isEmpty();
        assertThat(mentionService.resolveMentions(null)).isEmpty();
    }
}
