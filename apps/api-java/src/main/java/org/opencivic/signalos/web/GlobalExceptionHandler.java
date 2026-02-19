package org.opencivic.signalos.web;

import org.opencivic.signalos.web.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // P1-8: Domain-specific mapping to prevent leaking internal details
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleAuth(BadCredentialsException ex) {
        return buildResponse("Invalid credentials provided.", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex) {
        return buildResponse("Access denied: You don't have the required permissions.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse("Validation failed: " + msg, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleDomain(RuntimeException ex) {
        // Return clean message for custom runtime exceptions
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        // Log stack trace here with correlationId in a real environment
        return buildResponse("An internal server error occurred. Reference: " + correlationId, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> buildResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(
            new ApiError(message, status.value(), LocalDateTime.now()),
            status
        );
    }
}
