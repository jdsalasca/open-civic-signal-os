package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyAccountRequest(
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Activation code is required")
    String code
) {}
