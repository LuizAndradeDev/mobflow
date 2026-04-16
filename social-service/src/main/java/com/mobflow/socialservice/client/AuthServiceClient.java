package com.mobflow.socialservice.client;

import com.mobflow.socialservice.exception.SocialServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AuthServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public AuthServiceClient(
            @Qualifier("authRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    public AuthUserSummaryResponse resolveRequiredByUsername(String username) {
        try {
            AuthUserSummaryResponse response = restClient.get()
                    .uri("/internal/auth/users/by-username/{username}", username)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(AuthUserSummaryResponse.class);

            if (response == null) {
                throw SocialServiceException.userNotFound();
            }
            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw SocialServiceException.userNotFound();
        } catch (RestClientException exception) {
            throw SocialServiceException.upstreamServiceError();
        }
    }

    public Map<String, AuthUserSummaryResponse> resolveByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            AuthUserSummaryResponse[] responses = restClient.post()
                    .uri("/internal/auth/users/resolve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .body(usernames)
                    .retrieve()
                    .body(AuthUserSummaryResponse[].class);

            if (responses == null) {
                return Collections.emptyMap();
            }

            return Arrays.stream(responses)
                    .collect(Collectors.toMap(
                            AuthUserSummaryResponse::username,
                            response -> response,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
        } catch (RestClientException exception) {
            return Collections.emptyMap();
        }
    }

    public record AuthUserSummaryResponse(UUID authId, String username) {
    }
}
