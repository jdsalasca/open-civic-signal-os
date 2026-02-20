package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommunityThreadMessageRequest(
    @NotNull UUID sourceCommunityId,
    @NotBlank String content
) {}
