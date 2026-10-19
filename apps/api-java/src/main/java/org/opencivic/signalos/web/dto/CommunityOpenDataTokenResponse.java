package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityOpenDataTokenResponse(
    UUID id,
    String label,
    String tokenPrefix,
    List<String> scopes,
    int rateLimitPerHour,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime lastUsedAt,
    LocalDateTime revokedAt
) {}
