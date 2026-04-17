package com.mobflow.chatservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

    private String code;
    private String message;
    private Instant timestamp;

    public static ErrorResponseDTO of(String code, String message) {
        return ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
