package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommunityRequest(
    @NotBlank String name,
    String slug,
    String description,
    String country,
    String region,
    String city,
    String locality,
    String neighborhood,
    UUID parentCommunityId
) {}
