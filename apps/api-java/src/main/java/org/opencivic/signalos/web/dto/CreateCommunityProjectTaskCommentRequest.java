package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityProjectTaskCommentRequest(
    @NotBlank @Size(min = 3, max = 1000) String content
) {}
