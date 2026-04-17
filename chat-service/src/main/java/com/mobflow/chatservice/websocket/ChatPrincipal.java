package com.mobflow.chatservice.websocket;

import com.mobflow.chatservice.exception.ChatServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChatPrincipal implements Principal {

    private final UUID authId;
    private final String username;

    @Override
    public String getName() {
        return authId.toString();
    }

    public static ChatPrincipal from(Principal principal) {
        if (principal instanceof ChatPrincipal chatPrincipal) {
            return chatPrincipal;
        }
        throw ChatServiceException.websocketAuthenticationRequired();
    }
}
