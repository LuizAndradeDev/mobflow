package com.mobflow.authservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.security.SignatureException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setProperty("description", "The username or password is incorrect");
        return problem;
    }

    @ExceptionHandler(AccountStatusException.class)
    public ProblemDetail handleAccountStatus(AccountStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("description", "The account is locked or disabled");
        return problem;
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

        ex.printStackTrace();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problem.setProperty("description", "An unexpected error occurred. Please contact support.");
        return problem;
    }
}
