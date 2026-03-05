package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequest(
    @NotBlank(message = "Username is required")
    String username
) {}
