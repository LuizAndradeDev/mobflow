package com.mobflow.chatservice.client;

import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.resilience.InternalCallPolicy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final InternalCallPolicy friendshipPolicy;

    @Autowired
    public SocialServiceClient(
            @Qualifier("socialRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret,
            @Qualifier("socialFriendshipPolicy") InternalCallPolicy friendshipPolicy
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
        this.friendshipPolicy = friendshipPolicy;
    }

    public SocialServiceClient(RestClient restClient, String internalSecret) {
        this(restClient, internalSecret, InternalCallPolicy.noOp());
    }

    public void validateFriendshipRequired(UUID authId, UUID targetAuthId) {
        try {
            friendshipPolicy.execute(() -> restClient.get()
                    .uri("/social/internal/social/friendships/{authId}/friends/{targetAuthId}", authId, targetAuthId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .toBodilessEntity());
        } catch (HttpClientErrorException.NotFound exception) {
            throw ChatServiceException.friendshipRequired();
        } catch (CallNotPermittedException exception) {
            throw ChatServiceException.socialServiceUnavailable();
        } catch (RestClientException exception) {
            throw ChatServiceException.socialServiceUnavailable();
        }
    }
}
