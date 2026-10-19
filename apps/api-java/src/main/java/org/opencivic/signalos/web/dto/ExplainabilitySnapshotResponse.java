package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A frozen, self-verifying capture of a ranked list.
 *
 * <p>{@code payload} is the canonical JSON that {@code contentHash} is computed over, returned
 * verbatim so a reader can recompute the hash without trusting the server's arithmetic.
 */
public record ExplainabilitySnapshotResponse(
    UUID snapshotId,
    UUID communityId,
    String communityName,
    String label,
    UUID createdBy,
    LocalDateTime createdAt,
    String formulaVersion,
    int entryCount,
    String contentHash,
    Boolean verified,
    String verificationNote,
    String payload
) {}