package com.mobflow.workspaceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalSecret;

    public UserServiceClient(
            @Value("${user.service.url}") String userServiceUrl,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    public UUID resolveAuthIdByUsername(String username) {
        try {
            UserProfileResponse response = restClient.get()
                    .uri("/internal/users/by-username/{username}", username)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(UserProfileResponse.class);

            return (response != null) ? response.authId() : null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public Map<UUID, UserProfileResponse> fetchProfilesBatch(List<UUID> authIds) {
        if (authIds == null || authIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            UserProfileResponse[] responses = restClient.post()
                    .uri("/internal/users/batch")
                    .header("X-Internal-Secret", internalSecret)
                    .header("Content-Type", "application/json")
                    .body(authIds)
                    .retrieve()
                    .body(UserProfileResponse[].class);

            if (responses == null) return Collections.emptyMap();

            return java.util.Arrays.stream(responses)
                    .collect(Collectors.toMap(UserProfileResponse::authId, r -> r));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public record UserProfileResponse(UUID authId, String displayName, String avatarUrl) {}
}
