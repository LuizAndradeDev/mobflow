package com.mobflow.workspaceservice.config;

import com.mobflow.workspaceservice.exception.UserServiceUnavailableException;
import com.mobflow.workspaceservice.resilience.InternalCallPolicy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalSecret;
    private final InternalCallPolicy userLookupPolicy;
    private final InternalCallPolicy profileLookupPolicy;

    public UserServiceClient(
            @Qualifier("userRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret,
            @Qualifier("userLookupPolicy") InternalCallPolicy userLookupPolicy,
            @Qualifier("userProfileLookupPolicy") InternalCallPolicy profileLookupPolicy
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
        this.userLookupPolicy = userLookupPolicy;
        this.profileLookupPolicy = profileLookupPolicy;
    }

    public UUID resolveAuthIdByUsername(String username) {
        try {
            UserProfileResponse response = userLookupPolicy.execute(() -> restClient.get()
                    .uri("/internal/users/by-username/{username}", username)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(UserProfileResponse.class));

            return (response != null) ? response.authId() : null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (CallNotPermittedException | RestClientException e) {
            throw new UserServiceUnavailableException();
        }
    }

    public Map<UUID, UserProfileResponse> fetchProfilesBatch(List<UUID> authIds) {
        if (authIds == null || authIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            UserProfileResponse[] responses = profileLookupPolicy.execute(() -> restClient.post()
                    .uri("/internal/users/batch")
                    .header("X-Internal-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(authIds)
                    .retrieve()
                    .body(UserProfileResponse[].class));

            if (responses == null) return Collections.emptyMap();

            return java.util.Arrays.stream(responses)
                    .collect(Collectors.toMap(UserProfileResponse::authId, r -> r));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public record UserProfileResponse(UUID authId, String displayName, String avatarUrl) {}
}
