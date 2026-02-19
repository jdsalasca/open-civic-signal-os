package org.opencivic.signalos.web.error;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
    String message,
    int status,
    LocalDateTime timestamp,
    Map<String, String> details
) {
    public ApiError(String message, int status) {
        this(message, status, LocalDateTime.now(), null);
    }
    
    public ApiError(String message, int status, Map<String, String> details) {
        this(message, status, LocalDateTime.now(), details);
    }
}
