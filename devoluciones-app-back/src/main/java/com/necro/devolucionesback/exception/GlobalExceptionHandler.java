package com.necro.devolucionesback.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        log.error("ResponseStatusException: {}", ex.getReason());
        return buildResponse(ex.getStatusCode().value(), getErrorLabel(ex.getStatusCode().value()),
                ex.getReason(), request.getServletPath());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.error("UserAlreadyExistsException: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT.value(), "Conflict",
                ex.getMessage(), request.getServletPath());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStateTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        log.error("InvalidStateTransitionException: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT.value(), "Conflict",
                ex.getMessage(), request.getServletPath());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation error: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                errors, request.getServletPath());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.error("Constraint violation: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                errors, request.getServletPath());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                ex.getMessage(), request.getServletPath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "An unexpected error occurred", request.getServletPath());
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> runtimeException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "An unexpected error occurred", request.getServletPath());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int status, String error, String message, String path) {
        Map<String, Object> body = Map.of(
                "timestamp", java.time.LocalDateTime.now().toString(),
                "status", status,
                "error", error,
                "message", message != null ? message : "",
                "path", path != null ? path : ""
        );
        return ResponseEntity.status(status).body(body);
    }

    private String getErrorLabel(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            default -> "Internal Server Error";
        };
    }
}
