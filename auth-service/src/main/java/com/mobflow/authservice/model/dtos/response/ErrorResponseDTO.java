package com.mobflow.authservice.model.dtos.response;

import com.mobflow.authservice.model.enums.ErrorTP;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponseDTO {
    private ErrorTP errorType;
    private LocalDateTime timestamp;

    public static ErrorResponseDTO createErrorResponse(ErrorTP errorType, LocalDateTime timestamp) {
        return ErrorResponseDTO.builder().errorType(errorType).timestamp(timestamp).build();
    }
}
