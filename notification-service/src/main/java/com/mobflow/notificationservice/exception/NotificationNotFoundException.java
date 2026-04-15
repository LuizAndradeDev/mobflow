package com.mobflow.notificationservice.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(String notificationId) {
        super("Notification not found: " + notificationId);
    }
}
