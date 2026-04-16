package com.mobflow.socialservice.service;

import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

@Component
public class FriendRequestFactory {

    public FriendRequest create(
            AuthenticatedUser requester,
            AuthServiceClient.AuthUserSummaryResponse target
    ) {
        return FriendRequest.create(
                requester.authId(),
                requester.username(),
                target.authId(),
                target.username()
        );
    }
}
