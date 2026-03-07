package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record CastCommunityProposalVoteRequest(
    @Pattern(regexp = "FOR|AGAINST") String choice,
    @Min(1) @Max(5) Integer scoreValue
) {}
