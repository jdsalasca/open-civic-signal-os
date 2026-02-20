package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommunityRequest(
    @NotBlank String name,
    @NotBlank String slug,
    String description
) {}
