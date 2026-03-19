package com.mobflow.workspaceservice.exception;

public class MemberAlreadyExistsException extends RuntimeException {
    public MemberAlreadyExistsException() {
        super("MEMBER_ALREADY_EXISTS");
    }
}
