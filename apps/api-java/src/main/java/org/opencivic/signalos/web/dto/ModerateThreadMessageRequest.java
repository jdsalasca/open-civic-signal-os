package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerateThreadMessageRequest(
    boolean hidden,
    @NotBlank String reason
) {}
