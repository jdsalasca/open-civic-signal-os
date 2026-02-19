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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    // P0-1: Correct import from .dto package
    // P1-7: Improvement in semantic error mapping

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleAuth(BadCredentialsException ex) {
        return buildResponse("Authentication failed: Invalid credentials.", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex) {
        return buildResponse("Access denied: Insufficient permissions for this operation.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> details.put(e.getField(), e.getDefaultMessage()));
        
        return new ResponseEntity<>(
            new ApiError("Request validation failed", HttpStatus.BAD_REQUEST.value(), details),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegal(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleDomain(RuntimeException ex) {
        // P1-8: Generic clean message for internal domain errors
        return buildResponse("A domain error occurred: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        // In prod, log actual stack trace with correlationId
        return buildResponse("An unexpected error occurred. Reference: " + correlationId, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> buildResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(
            new ApiError(message, status.value()),
            status
        );
    }
}
