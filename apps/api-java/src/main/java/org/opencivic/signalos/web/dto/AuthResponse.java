package org.opencivic.signalos.web.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String role, // This will contain the comma-separated string for FE to parse
    String username
) {}
