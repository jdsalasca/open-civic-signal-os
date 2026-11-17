package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommunityThreadRequest(
    @NotNull UUID sourceCommunityId,
    @NotNull UUID targetCommunityId,
    UUID relatedSignalId,
    @NotBlank String title,
    @Size(max = 2000) String initialMessage
) {}
