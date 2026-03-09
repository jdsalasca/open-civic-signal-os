package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerateCommunityReportRequest(
    @NotBlank String action,
    boolean hideContent,
    @Size(max = 40) String sanctionType,
    @NotBlank @Size(min = 8, max = 2000) String resolutionReason
) {}