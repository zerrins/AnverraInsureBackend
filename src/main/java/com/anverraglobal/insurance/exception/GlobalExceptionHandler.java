package com.anverraglobal.insurance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, 
                "Invalid email or password"
        );
        problemDetail.setTitle("Unauthorized");
        problemDetail.setProperty("error", "Invalid email or password");
        problemDetail.setType(URI.create("https://api.anverraglobal.com/errors/unauthorized"));
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                ex.getMessage() != null ? ex.getMessage() : "Bad Request"
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("error", ex.getMessage());
        problemDetail.setType(URI.create("https://api.anverraglobal.com/errors/bad-request"));
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex) {
        ex.printStackTrace();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "An unexpected error occurred");
        problemDetail.setType(URI.create("https://api.anverraglobal.com/errors/internal-server-error"));
        return problemDetail;
    }
}
