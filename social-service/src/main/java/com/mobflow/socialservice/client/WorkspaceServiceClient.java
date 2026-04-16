package com.mobflow.socialservice.client;

import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.model.enums.WorkspaceRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class WorkspaceServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public WorkspaceServiceClient(
            @Qualifier("workspaceRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    public WorkspaceRole requireMembership(UUID workspaceId, UUID authId) {
        try {
            MemberRoleResponse response = restClient.get()
                    .uri("/internal/workspaces/{workspaceId}/members/{authId}/role", workspaceId, authId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(MemberRoleResponse.class);

            if (response == null) {
                throw SocialServiceException.membershipRequired();
            }
            return WorkspaceRole.valueOf(response.role());
        } catch (HttpClientErrorException.NotFound exception) {
            throw SocialServiceException.membershipRequired();
        } catch (RestClientException exception) {
            throw SocialServiceException.upstreamServiceError();
        }
    }

    public boolean isOwnerOrAdmin(UUID workspaceId, UUID authId) {
        WorkspaceRole role = requireMembership(workspaceId, authId);
        return role == WorkspaceRole.OWNER || role == WorkspaceRole.ADMIN;
    }

    public record MemberRoleResponse(String role) {
    }
}
