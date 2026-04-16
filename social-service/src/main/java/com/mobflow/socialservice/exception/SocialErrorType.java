package com.mobflow.socialservice.exception;

import org.springframework.http.HttpStatus;

public enum SocialErrorType {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Comment not found"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Friend request not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You are not allowed to perform this action"),
    WORKSPACE_MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "You must be a workspace member to perform this action"),
    COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "Comment is already deleted"),
    INVALID_COMMENT_STATE(HttpStatus.BAD_REQUEST, "Comment cannot be edited in its current state"),
    FRIEND_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself"),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "A pending friend request already exists for these users"),
    FRIENDSHIP_ALREADY_EXISTS(HttpStatus.CONFLICT, "Friendship already exists"),
    INVALID_FRIEND_REQUEST_STATE(HttpStatus.BAD_REQUEST, "Friend request cannot be processed in its current state"),
    UPSTREAM_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "Unable to validate required information with another service");

    private final HttpStatus status;
    private final String message;

    SocialErrorType(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
