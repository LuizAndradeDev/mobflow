package com.mobflow.taskservice.client;

import com.mobflow.taskservice.exception.TaskServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * HTTP client for internal communication with workspace-service.
 * Validates that a user is a member of a workspace and checks their role.
 */
@Component
public class WorkspaceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public WorkspaceClient(
            @Qualifier("workspaceRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    /**
     * Returns the role of a member in a workspace.
     * Throws TaskServiceException (memberNotFound) if the user is not a member.
     */
    public MemberRoleResponse getMemberRole(UUID workspaceId, UUID authId) {
        try {
            return restClient.get()
                    .uri("/internal/workspaces/{workspaceId}/members/{authId}/role", workspaceId, authId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(MemberRoleResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw TaskServiceException.memberNotFound();
        }
    }

    /**
     * Returns true if the member has OWNER or ADMIN role.
     */
    public boolean isOwnerOrAdmin(UUID workspaceId, UUID authId) {
        MemberRoleResponse response = getMemberRole(workspaceId, authId);
        if (response == null) return false;
        return "OWNER".equals(response.role()) || "ADMIN".equals(response.role());
    }

    public record MemberRoleResponse(String role) {}
}
