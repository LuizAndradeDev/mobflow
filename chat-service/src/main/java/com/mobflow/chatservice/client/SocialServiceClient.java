package com.mobflow.chatservice.client;

import com.mobflow.chatservice.exception.ChatServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class SocialServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public SocialServiceClient(
            @Qualifier("socialRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    public void validateFriendshipRequired(UUID authId, UUID targetAuthId) {
        try {
            restClient.get()
                    .uri("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", authId, targetAuthId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw ChatServiceException.friendshipRequired();
        } catch (RestClientException exception) {
            throw ChatServiceException.socialServiceUnavailable();
        }
    }
}
