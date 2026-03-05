package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommunityBlogPostRequest(
    @NotNull UUID communityId,
    @NotBlank String title,
    @NotBlank String content,
    @NotBlank String statusTag
) {}
