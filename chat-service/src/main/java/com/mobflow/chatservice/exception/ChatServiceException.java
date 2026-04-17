package com.mobflow.chatservice.exception;

import org.springframework.http.HttpStatus;

public class ChatServiceException extends RuntimeException {

    private final ChatErrorType errorType;

    public ChatServiceException(ChatErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public static ChatServiceException conversationNotFound() {
        return new ChatServiceException(ChatErrorType.CONVERSATION_NOT_FOUND);
    }

    public static ChatServiceException accessDenied() {
        return new ChatServiceException(ChatErrorType.ACCESS_DENIED);
    }

    public static ChatServiceException selfConversationNotAllowed() {
        return new ChatServiceException(ChatErrorType.SELF_CONVERSATION_NOT_ALLOWED);
    }

    public static ChatServiceException friendshipRequired() {
        return new ChatServiceException(ChatErrorType.FRIENDSHIP_REQUIRED);
    }

    public static ChatServiceException socialServiceUnavailable() {
        return new ChatServiceException(ChatErrorType.SOCIAL_SERVICE_UNAVAILABLE);
    }

    public static ChatServiceException invalidMessageContent() {
        return new ChatServiceException(ChatErrorType.INVALID_MESSAGE_CONTENT);
    }

    public static ChatServiceException websocketAuthenticationRequired() {
        return new ChatServiceException(ChatErrorType.WEBSOCKET_AUTHENTICATION_REQUIRED);
    }

    public static ChatServiceException invalidDestination() {
        return new ChatServiceException(ChatErrorType.INVALID_DESTINATION);
    }

    public ChatErrorType getErrorType() {
        return errorType;
    }

    public HttpStatus getStatus() {
        return errorType.getStatus();
    }

    public String getCode() {
        return errorType.getCode();
    }
}
