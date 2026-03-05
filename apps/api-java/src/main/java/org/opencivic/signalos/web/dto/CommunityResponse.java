package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityResponse(
    UUID id,
    String name,
    String slug,
    String description,
    UUID parentCommunityId,
    LocalDateTime createdAt
) {}
