package com.mobflow.userservice.model.dto.response;

import com.mobflow.userservice.exceptions.enums.ErrorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponseDTO {

    private ErrorType errorType;
    private LocalDateTime timestamp;

    public static ErrorResponseDTO of(ErrorType errorType, LocalDateTime timestamp) {
        return ErrorResponseDTO.builder()
                .errorType(errorType)
                .timestamp(timestamp)
                .build();
    }
}
