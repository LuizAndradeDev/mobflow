package com.mobflow.socialservice.controller;

import com.mobflow.socialservice.model.dto.request.SendFriendRequestRequest;
import com.mobflow.socialservice.model.dto.response.FriendRequestResponse;
import com.mobflow.socialservice.model.dto.response.FriendResponse;
import com.mobflow.socialservice.security.AuthenticatedUser;
import com.mobflow.socialservice.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FriendController {

    private final FriendshipService friendshipService;

    public FriendController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/api/friends/request")
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.sendFriendRequest(AuthenticatedUser.from(authentication), request));
    }

    @GetMapping("/api/friends/requests")
    public ResponseEntity<List<FriendRequestResponse>> listFriendRequests(Authentication authentication) {
        return ResponseEntity.ok(friendshipService.listFriendRequests(AuthenticatedUser.from(authentication)));
    }

    @PostMapping("/api/friends/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptFriendRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(friendshipService.acceptFriendRequest(
                requestId,
                AuthenticatedUser.from(authentication)
        ));
    }

    @PostMapping("/api/friends/{requestId}/decline")
    public ResponseEntity<FriendRequestResponse> declineFriendRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(friendshipService.declineFriendRequest(
                requestId,
                AuthenticatedUser.from(authentication)
        ));
    }

    @GetMapping("/api/friends")
    public ResponseEntity<List<FriendResponse>> listFriends(Authentication authentication) {
        return ResponseEntity.ok(friendshipService.listFriends(AuthenticatedUser.from(authentication)));
    }
}
