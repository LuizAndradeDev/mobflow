package com.mobflow.socialservice.exception;

import org.springframework.http.HttpStatus;

public class SocialServiceException extends RuntimeException {

    private final SocialErrorType errorType;

    public SocialServiceException(SocialErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public static SocialServiceException commentNotFound() {
        return new SocialServiceException(SocialErrorType.COMMENT_NOT_FOUND);
    }

    public static SocialServiceException taskNotFound() {
        return new SocialServiceException(SocialErrorType.TASK_NOT_FOUND);
    }

    public static SocialServiceException friendRequestNotFound() {
        return new SocialServiceException(SocialErrorType.FRIEND_REQUEST_NOT_FOUND);
    }

    public static SocialServiceException userNotFound() {
        return new SocialServiceException(SocialErrorType.USER_NOT_FOUND);
    }

    public static SocialServiceException accessDenied() {
        return new SocialServiceException(SocialErrorType.ACCESS_DENIED);
    }

    public static SocialServiceException membershipRequired() {
        return new SocialServiceException(SocialErrorType.WORKSPACE_MEMBERSHIP_REQUIRED);
    }

    public static SocialServiceException commentAlreadyDeleted() {
        return new SocialServiceException(SocialErrorType.COMMENT_ALREADY_DELETED);
    }

    public static SocialServiceException invalidCommentState() {
        return new SocialServiceException(SocialErrorType.INVALID_COMMENT_STATE);
    }

    public static SocialServiceException friendRequestToSelf() {
        return new SocialServiceException(SocialErrorType.FRIEND_REQUEST_TO_SELF);
    }

    public static SocialServiceException friendRequestAlreadyExists() {
        return new SocialServiceException(SocialErrorType.FRIEND_REQUEST_ALREADY_EXISTS);
    }

    public static SocialServiceException friendshipAlreadyExists() {
        return new SocialServiceException(SocialErrorType.FRIENDSHIP_ALREADY_EXISTS);
    }

    public static SocialServiceException invalidFriendRequestState() {
        return new SocialServiceException(SocialErrorType.INVALID_FRIEND_REQUEST_STATE);
    }

    public static SocialServiceException upstreamServiceError() {
        return new SocialServiceException(SocialErrorType.UPSTREAM_SERVICE_ERROR);
    }

    public SocialErrorType getErrorType() {
        return errorType;
    }

    public HttpStatus getStatus() {
        return errorType.getStatus();
    }
}
