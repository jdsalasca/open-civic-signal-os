package org.opencivic.signalos.service;

/**
 * Shared limit contract for community collection endpoints.
 *
 * ponytail: these endpoints return a single array, not a page envelope, because every
 * existing consumer expects that shape. This bounds the array instead of replacing the
 * shape. Moving to {@code ApiPageResponse} needs a frontend migration per endpoint; see
 * docs/architecture/ADR-20260321-community-list-limits-contract.md for which endpoints are
 * still unbounded.
 */
public final class CommunityListLimits {
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private CommunityListLimits() {}

    /**
     * Clamps a caller-supplied limit into the documented range. A null, zero, negative,
     * or oversized value falls back to the default rather than erroring, so a malformed
     * request degrades to a safe bounded read instead of failing.
     */
    public static int resolveLimit(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }
}