package com.mobflow.workspaceservice.exception;

public class WorkspaceNotFoundException extends RuntimeException {
    public WorkspaceNotFoundException() {
        super("WORKSPACE_NOT_FOUND");
    }
}
