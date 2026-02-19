package org.opencivic.signalos.web.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String role,
    String username
) {}
