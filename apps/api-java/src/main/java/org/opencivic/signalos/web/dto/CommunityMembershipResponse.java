package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityMembershipResponse(
    UUID userId,
    UUID communityId,
    String communityName,
    String communitySlug,
    String role,
    UUID createdBy,
    LocalDateTime createdAt
) {}
