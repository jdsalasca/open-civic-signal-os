package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommunityThreadRequest(
    @NotNull UUID sourceCommunityId,
    @NotNull UUID targetCommunityId,
    UUID relatedSignalId,
    @NotBlank String title
) {}
