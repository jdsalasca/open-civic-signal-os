package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotNull;
import org.opencivic.signalos.domain.CommunityRole;

public record JoinCommunityRequest(
    @NotNull CommunityRole role
) {}
