package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommunityBlogPostRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotBlank String statusTag
) {}
