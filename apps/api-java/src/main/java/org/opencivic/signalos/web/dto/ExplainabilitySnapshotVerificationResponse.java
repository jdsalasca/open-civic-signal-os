package org.opencivic.signalos.web.dto;

import java.util.UUID;

/**
 * Independent check that a snapshot's rows were not altered after creation.
 *
 * <p>{@code recomputedHash} is returned alongside {@code recordedHash} on purpose: a reader
 * should be able to see both and draw their own conclusion rather than accept a boolean from the
 * same party that produced the data.
 */
public record ExplainabilitySnapshotVerificationResponse(
    UUID snapshotId,
    String recordedHash,
    String recomputedHash,
    boolean valid,
    String explanation
) {}