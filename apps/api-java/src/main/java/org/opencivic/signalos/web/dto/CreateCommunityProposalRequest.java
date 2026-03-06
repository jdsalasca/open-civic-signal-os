package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateCommunityProposalRequest(
    @NotNull UUID communityId,
    UUID relatedSignalId,
    @NotBlank @Size(min = 8, max = 160) String title,
    @NotBlank @Size(min = 20, max = 4000) String problemStatement,
    @NotBlank @Size(min = 20, max = 4000) String proposedSolution,
    @NotBlank @Size(min = 5, max = 1200) String estimatedCost,
    @NotBlank @Size(min = 10, max = 2000) String beneficiariesSummary,
    @NotNull @Size(max = 5) List<@Size(max = 1200) String> supportingLinks
) {}
