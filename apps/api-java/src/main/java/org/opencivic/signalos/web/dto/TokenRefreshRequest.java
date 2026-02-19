package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank String refreshToken
) {}
