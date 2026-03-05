package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommunityRequest(
    @NotBlank String name,
    @NotBlank String slug,
    String description,
    UUID parentCommunityId
) {}
