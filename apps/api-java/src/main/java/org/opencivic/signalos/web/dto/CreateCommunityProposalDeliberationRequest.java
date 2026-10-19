package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.opencivic.signalos.domain.CommunityProposalDeliberationType;

public record CreateCommunityProposalDeliberationRequest(
    @NotNull CommunityProposalDeliberationType type,
    @NotBlank @Size(min = 8, max = 2000) String content,
    @Size(max = 1200) String supportingLink
) {}
