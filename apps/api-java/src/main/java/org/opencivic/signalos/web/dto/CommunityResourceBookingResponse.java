package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityResourceBookingResponse(
    UUID id,
    UUID resourceId,
    String resourceName,
    UUID communityId,
    UUID requesterId,
    String requesterName,
    String purpose,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String status,
    String decisionNote,
    LocalDateTime requestedAt,
    LocalDateTime decidedAt,
    String decidedByName,
    LocalDateTime cancelledAt
) {}