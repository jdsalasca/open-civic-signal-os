package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityResponse(
    UUID id,
    String name,
    String slug,
    String description,
    String country,
    String region,
    UUID parentCommunityId,
    long memberCount,
    LocalDateTime createdAt
) {}
