package org.opencivic.signalos.exception;

import java.time.LocalDateTime;

public class TooManyRequestsException extends RuntimeException {
    private final LocalDateTime resetAt;

    public TooManyRequestsException(String message, LocalDateTime resetAt) {
        super(message);
        this.resetAt = resetAt;
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }
}
