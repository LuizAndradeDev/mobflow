package com.mobflow.authservice.exceptions;

import com.mobflow.authservice.model.dtos.response.ErrorResponseDTO;
import com.mobflow.authservice.model.enums.ErrorTP;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.security.SignatureException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDTO.createErrorResponse(ErrorTP.INVALID_CREDENTIALS, LocalDateTime.now()));
    }

    @ExceptionHandler(AccountStatusException.class)
    public ProblemDetail handleAccountStatus(AccountStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("description", "The account is locked or disabled");
        return problem;
    }

    @ExceptionHandler(GenericAplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleUniqueViolation(GenericAplicationException e) {
        ErrorTP errorTP = ErrorTP.valueOf(e.getMessage());
        ErrorResponseDTO error = ErrorResponseDTO.createErrorResponse(errorTP, LocalDateTime.now());
        log.warn("Handled authentication business error: {}", errorTP);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({SignatureException.class, ExpiredJwtException.class})
    public ProblemDetail handleJwtExceptions(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        String desc = (ex instanceof ExpiredJwtException) ? "The JWT token has expired" : "The JWT signature is invalid";
        problem.setProperty("description", desc);
        return problem;
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("description", "You are not authorized to access this resource");
        return problem;
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setProperty("description", "The resource or endpoint does not exist");
        return problem;
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled authentication service exception", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problem.setProperty("description", "An unexpected error occurred. Please contact support.");
        return problem;
    }
}
