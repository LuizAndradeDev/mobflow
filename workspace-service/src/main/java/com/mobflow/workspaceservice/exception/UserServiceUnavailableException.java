package com.mobflow.workspaceservice.exception;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException() {
        super("USER_SERVICE_UNAVAILABLE");
    }
}
