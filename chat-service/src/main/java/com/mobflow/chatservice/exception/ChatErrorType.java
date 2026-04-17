package com.mobflow.chatservice.exception;

import org.springframework.http.HttpStatus;

public enum ChatErrorType {
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to access this conversation"),
    SELF_CONVERSATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SELF_CONVERSATION_NOT_ALLOWED", "You cannot create a conversation with yourself"),
    FRIENDSHIP_REQUIRED(HttpStatus.FORBIDDEN, "FRIENDSHIP_REQUIRED", "Only friends can create conversations or exchange messages"),
    SOCIAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SOCIAL_SERVICE_UNAVAILABLE", "Unable to validate friendship with social-service"),
    INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "INVALID_MESSAGE_CONTENT", "Message content must not be blank"),
    WEBSOCKET_AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "WEBSOCKET_AUTHENTICATION_REQUIRED", "A valid JWT is required to establish the WebSocket session"),
    INVALID_DESTINATION(HttpStatus.BAD_REQUEST, "INVALID_DESTINATION", "The requested WebSocket destination is invalid");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ChatErrorType(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
