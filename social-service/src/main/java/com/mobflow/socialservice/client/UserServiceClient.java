package com.mobflow.socialservice.client;

import com.mobflow.socialservice.resilience.InternalCallPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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
    private final InternalCallPolicy profileLookupPolicy;

    @Autowired
    public UserServiceClient(
            @Qualifier("userRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret,
            @Qualifier("userProfileLookupPolicy") InternalCallPolicy profileLookupPolicy
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
        this.profileLookupPolicy = profileLookupPolicy;
    }

    public UserServiceClient(RestClient restClient, String internalSecret) {
        this(restClient, internalSecret, InternalCallPolicy.noOp());
    }

    public List<UserProfileResponse> fetchProfiles(List<UUID> authIds) {
        if (authIds == null || authIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<UserProfileResponse> result = profileLookupPolicy.execute(() -> restClient.post()
                    .uri("/internal/users/batch")
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(authIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {}));
            return result != null ? result : Collections.emptyList();
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    public record UserProfileResponse(UUID authId, String displayName, String avatarUrl) {
    }
}
