package com.mobflow.notificationservice.exception;

import com.mobflow.notificationservice.model.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotificationNotFound(NotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDTO.of("NOTIFICATION_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(NotificationBusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessError(NotificationBusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception exception) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problemDetail.setProperty("description", exception.getMessage());
        return problemDetail;
    }
}
