package com.mobflow.socialservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class UserServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public UserServiceClient(
            @Qualifier("userRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    public List<UserProfileResponse> fetchProfiles(List<UUID> authIds) {
        if (authIds == null || authIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<UserProfileResponse> result = restClient.post()
                    .uri("/internal/users/batch")
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .body(authIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    public record UserProfileResponse(UUID authId, String displayName, String avatarUrl) {
    }
}
