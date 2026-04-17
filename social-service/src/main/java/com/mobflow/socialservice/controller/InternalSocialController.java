package com.mobflow.socialservice.controller;

import com.mobflow.socialservice.service.FriendshipService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/social")
public class InternalSocialController {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final FriendshipService friendshipService;
    private final String internalSecret;

    public InternalSocialController(
            FriendshipService friendshipService,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.friendshipService = friendshipService;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/friendships/{authId}/friends/{targetAuthId}")
    public ResponseEntity<Void> validateFriendship(
            @PathVariable UUID authId,
            @PathVariable UUID targetAuthId,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (friendshipService.areFriends(authId, targetAuthId)) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
}
