package com.chatpaykit.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------
    // Custom ApiException (recommended)
    // -------------------------
    @ExceptionHandler(ApiException.class)
    @ResponseStatus
    public Map<String, Object> handleApi(ApiException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", ex.getStatus().value(),
                "error", ex.getStatus().getReasonPhrase(),
                "message", ex.getMessage()
        );
    }

    // -------------------------
    // Bad Request
    // -------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequestState(IllegalStateException ex) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        );
    }

    // -------------------------
// Validation errors (400)
// -------------------------
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Map<String, Object> validation(MethodArgumentNotValidException ex) {

    Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (a, b) -> a
            ));

    return Map.of(
            "timestamp", Instant.now().toString(),
            "status", 400,
            "error", "Bad Request",
            "message", "Validation failed",
            "fields", fieldErrors
    );
}

// -------------------------
// Missing admin header (400)
// -------------------------
@ExceptionHandler(MissingRequestHeaderException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Map<String, Object> missingHeader(MissingRequestHeaderException ex) {
    return Map.of(
            "timestamp", Instant.now().toString(),
            "status", 400,
            "error", "Bad Request",
            "message", ex.getMessage()
    );
}


    // -------------------------
    // Everything else (500)
    // -------------------------
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> serverError(Exception ex) {

        // Log full stack trace internally
        log.error("Unhandled exception", ex);

        // Do NOT expose root cause in production
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "Something went wrong. Please contact support."
        );
    }
}
