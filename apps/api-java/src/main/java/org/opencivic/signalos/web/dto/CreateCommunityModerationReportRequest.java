package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityModerationReasonCode;
import org.opencivic.signalos.domain.CommunityModerationTargetType;

public record CreateCommunityModerationReportRequest(
    @NotNull UUID communityId,
    @NotNull CommunityModerationTargetType targetType,
    @NotNull UUID targetId,
    @NotNull CommunityModerationReasonCode reasonCode,
    @NotBlank @Size(min = 8, max = 2000) String details
) {}