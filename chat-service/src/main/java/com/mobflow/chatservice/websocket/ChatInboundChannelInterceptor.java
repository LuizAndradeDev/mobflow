package com.mobflow.chatservice.websocket;

import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.security.JwtService;
import com.mobflow.chatservice.service.ConversationGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatInboundChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ConversationGuardService conversationGuardService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        boolean mutated = false;

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
            mutated = true;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
            mutated = true;
        }

        if (StompCommand.SEND.equals(command)) {
            restoreUserIfNecessary(accessor);
            mutated = true;
        }

        if (!mutated) {
            return message;
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String rawToken = resolveToken(accessor);
        if (rawToken == null || rawToken.isBlank()) {
            throw ChatServiceException.websocketAuthenticationRequired();
        }

        String token = rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
        String username = jwtService.extractUsername(token);
        UUID authId = jwtService.extractAuthId(token);

        if (username == null || authId == null || !jwtService.isTokenValid(token)) {
            throw ChatServiceException.websocketAuthenticationRequired();
        }

        accessor.setUser(new ChatPrincipal(authId, username));
        accessor.getSessionAttributes().put("authId", authId.toString());
        accessor.getSessionAttributes().put("username", username);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        ChatPrincipal principal = restoreUserIfNecessary(accessor);
        String destination = accessor.getDestination();

        if (destination == null) {
            throw ChatServiceException.invalidDestination();
        }

        if (!destination.startsWith("/topic/conversations/")) {
            return;
        }

        String conversationIdValue = destination.substring("/topic/conversations/".length());
        try {
            UUID conversationId = UUID.fromString(conversationIdValue);
            conversationGuardService.requireParticipant(conversationId, principal.getAuthId());
        } catch (IllegalArgumentException exception) {
            throw ChatServiceException.invalidDestination();
        }
    }

    private ChatPrincipal restoreUserIfNecessary(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof ChatPrincipal chatPrincipal) {
            return chatPrincipal;
        }

        if (accessor.getSessionAttributes() == null) {
            throw ChatServiceException.websocketAuthenticationRequired();
        }

        Object authIdValue = accessor.getSessionAttributes().get("authId");
        Object usernameValue = accessor.getSessionAttributes().get("username");

        if (authIdValue == null || usernameValue == null) {
            throw ChatServiceException.websocketAuthenticationRequired();
        }

        ChatPrincipal restoredPrincipal = new ChatPrincipal(UUID.fromString(authIdValue.toString()), usernameValue.toString());
        accessor.setUser(restoredPrincipal);
        return restoredPrincipal;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
        if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
            return authorizationHeaders.getFirst();
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.getFirst();
        }

        return null;
    }
}
