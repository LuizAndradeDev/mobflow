package com.mobflow.socialservice.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SocialServiceException.class)
    public ProblemDetail handleSocialServiceException(SocialServiceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getErrorType().name());
        problem.setType(URI.create("https://api.mobflow.com/errors/social-service/" + exception.getErrorType().name().toLowerCase()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
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
        exception.printStackTrace();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.mobflow.com/errors/internal-server-error"));
        problem.setProperty("description", "An unexpected error occurred. Please contact support.");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
