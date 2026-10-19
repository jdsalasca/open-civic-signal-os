package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to freeze the current ranked list as a verifiable snapshot.
 *
 * <p>{@code label} is required and free text because a snapshot cited in meeting minutes needs
 * to say what meeting it was for. An unlabelled snapshot is unfindable six months later.
 */
public record ExplainabilitySnapshotCreateRequest(
    @NotNull UUID communityId,
    @NotBlank String label,
    Integer limit
) {}