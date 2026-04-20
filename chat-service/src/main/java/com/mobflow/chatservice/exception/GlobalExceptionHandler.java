package com.mobflow.chatservice.exception;

import com.mobflow.chatservice.model.dto.response.ErrorResponseDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<ErrorResponseDTO> handleChatServiceException(ChatServiceException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponseDTO.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.mobflow.com/errors/validation"));
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({ExpiredJwtException.class, JwtException.class})
    public ProblemDetail handleJwtExceptions(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Authentication Error");
        problem.setType(URI.create("https://api.mobflow.com/errors/authentication"));
        problem.setProperty("description", exception instanceof ExpiredJwtException
                ? "The JWT token has expired"
                : "The JWT token is invalid");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://api.mobflow.com/errors/access-denied"));
        problem.setProperty("description", "You are not authorized to access this resource");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://api.mobflow.com/errors/not-found"));
        problem.setProperty("description", "The resource or endpoint does not exist");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception exception) {
        log.error("Unhandled chat service exception", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.mobflow.com/errors/internal-server-error"));
        problem.setProperty("description", "An unexpected error occurred. Please contact support.");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @MessageExceptionHandler(ChatServiceException.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDTO handleWebSocketChatServiceException(ChatServiceException exception) {
        return ErrorResponseDTO.of(exception.getCode(), exception.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDTO handleWebSocketGenericException(Exception exception) {
        log.error("Unhandled chat WebSocket exception", exception);
        return ErrorResponseDTO.of("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please contact support.");
    }
}
