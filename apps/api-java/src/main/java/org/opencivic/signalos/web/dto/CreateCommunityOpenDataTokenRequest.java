package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreateCommunityOpenDataTokenRequest(
    UUID communityId,
    @NotBlank String label,
    @NotEmpty List<String> scopes,
    @Min(1) @Max(1000) int rateLimitPerHour
) {}
