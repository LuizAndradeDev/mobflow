package com.mobflow.notificationservice.dto.response;

import java.time.Instant;

public record ErrorResponseDTO(
        String code,
        String message,
        Instant timestamp
) {
    public static ErrorResponseDTO of(String code, String message) {
        return new ErrorResponseDTO(code, message, Instant.now());
    }
}
