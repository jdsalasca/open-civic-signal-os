package org.opencivic.signalos.domain;

public enum SignalStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    FLAGGED,
    REJECTED;

    public boolean canTransitionTo(SignalStatus next) {
        if (this == RESOLVED || this == REJECTED) return false;
        if (this == FLAGGED) return next == NEW || next == REJECTED;
        return true;
    }
}
