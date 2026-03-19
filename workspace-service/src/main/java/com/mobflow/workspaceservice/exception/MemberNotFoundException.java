package com.mobflow.workspaceservice.exception;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException() {
        super("MEMBER_NOT_FOUND");
    }
}
