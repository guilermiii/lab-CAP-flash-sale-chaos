package com.chaoslab.flashsale.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SoldOutException.class)
    public ProblemDetail handleSoldOutException(SoldOutException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Sold Out");
        problemDetail.setType(URI.create("urn:problem-type:sold-out"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler({
            ServiceUnavailableException.class,
            org.springframework.transaction.CannotCreateTransactionException.class,
            org.springframework.dao.DataAccessException.class
    })
    public ProblemDetail handleServiceUnavailableException(Exception ex) {
        String detail = (ex instanceof ServiceUnavailableException && ex.getMessage() != null)
                ? ex.getMessage()
                : "Database unavailable or lock timeout";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(URI.create("urn:problem-type:service-unavailable"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("urn:problem-type:bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
