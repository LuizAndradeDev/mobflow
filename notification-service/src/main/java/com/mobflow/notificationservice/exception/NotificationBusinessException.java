package com.mobflow.notificationservice.exception;

public class NotificationBusinessException extends RuntimeException {
    private final String code;

    public NotificationBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
