package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.client.WorkspaceServiceClient;
import com.mobflow.socialservice.exception.SocialErrorType;
import com.mobflow.socialservice.exception.SocialServiceException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,50})");

    private final AuthServiceClient authServiceClient;
    private final WorkspaceServiceClient workspaceServiceClient;

    public MentionService(
            AuthServiceClient authServiceClient,
            WorkspaceServiceClient workspaceServiceClient
    ) {
        this.authServiceClient = authServiceClient;
        this.workspaceServiceClient = workspaceServiceClient;
    }

    public List<ResolvedMention> resolveMentions(String content, java.util.UUID workspaceId) {
        Set<String> usernames = extractMentionUsernames(content);
        if (usernames.isEmpty()) {
            return List.of();
        }

        Map<String, AuthServiceClient.AuthUserSummaryResponse> resolvedUsers =
                authServiceClient.resolveByUsernames(new ArrayList<>(usernames));

        List<ResolvedMention> resolvedMentions = new ArrayList<>();
        try {
            for (String username : usernames) {
                AuthServiceClient.AuthUserSummaryResponse resolvedUser = resolvedUsers.get(username);
                if (resolvedUser != null && workspaceServiceClient.isWorkspaceMember(workspaceId, resolvedUser.authId())) {
                    resolvedMentions.add(new ResolvedMention(resolvedUser.authId(), resolvedUser.username()));
                }
            }
        } catch (SocialServiceException exception) {
            if (exception.getErrorType() == SocialErrorType.UPSTREAM_SERVICE_ERROR) {
                return List.of();
            }
            throw exception;
        }
        return resolvedMentions;
    }

    private Set<String> extractMentionUsernames(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> usernames = new LinkedHashSet<>();
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }

    public record ResolvedMention(
            java.util.UUID authId,
            String username
    ) {
    }
}
