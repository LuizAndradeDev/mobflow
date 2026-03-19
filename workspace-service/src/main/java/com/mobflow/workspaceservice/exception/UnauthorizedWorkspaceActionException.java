package com.mobflow.workspaceservice.exception;

public class UnauthorizedWorkspaceActionException extends RuntimeException {
    public UnauthorizedWorkspaceActionException() {
        super("UNAUTHORIZED_ACTION");
    }
}
