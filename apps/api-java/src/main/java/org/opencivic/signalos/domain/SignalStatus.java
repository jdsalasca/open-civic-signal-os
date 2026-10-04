package org.opencivic.signalos.domain;

import java.util.Locale;

/**
 * A signal's lifecycle state, and the one definition of whether it is settled.
 *
 * <p>The settled question used to have an answer per service: seven of them each carried
 * {@code Set.of("RESOLVED", "CLOSED", "REJECTED")} as a private constant, and a federation export added
 * an eighth written against this enum. Because {@code CLOSED} predates the enum and is not a member of
 * it, the eighth disagreed with the other seven: a signal whose status was the legacy
 * {@code "CLOSED"} appeared in the federated backlog while six views showed it resolved.
 *
 * <p>Nothing crashed. One public view simply answered a different question from the others, which is
 * the worst way for this to break. So the answer lives here, next to the enum, and every caller asks.
 *
 * <p>Prioritization drift is the same failure with arithmetic instead of a filter, and the weights
 * already moved for it: see {@link PrioritizationFormula} and the ADR behind it.
 */
public enum SignalStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    FLAGGED,
    REJECTED;

    /**
     * Legacy status strings that predate this enum and still appear in stored rows.
     *
     * <p>Not decoration: a ranking that stopped honouring {@code CLOSED} would resurrect a settled
     * issue in whichever view changed, which is how the two definitions drifted in the first place.
     */
    private static final java.util.Set<String> LEGACY_SETTLED = java.util.Set.of("CLOSED");

    public boolean canTransitionTo(SignalStatus next) {
        if (this == RESOLVED || this == REJECTED) return false;
        if (this == FLAGGED) return next == NEW || next == REJECTED;
        return true;
    }

    /** Whether the community has settled this issue, as opposed to it still being open. */
    public boolean isSettled() {
        return this == RESOLVED || this == REJECTED;
    }

    /**
     * Whether a stored status string means the issue is settled.
     *
     * <p>Tolerant of case and surrounding space, because the strings come from data rather than from
     * this enum. An unknown or missing status is <b>not</b> settled: a public view should not quietly
     * drop a resident's report because its status is one this version has not heard of.
     */
    public static boolean isSettled(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return false;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_SETTLED.contains(normalized)) {
            return true;
        }
        try {
            return valueOf(normalized).isSettled();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}