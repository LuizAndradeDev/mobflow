package com.mobflow.authservice.domain.model.dtos;

import com.mobflow.authservice.domain.model.enums.ErrorTP;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponseDTO {
    private ErrorTP errorType;
    private LocalDateTime timestamp;
}
