package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerateCommunityProposalEntryRequest(
    boolean hidden,
    @NotBlank String reason
) {}
