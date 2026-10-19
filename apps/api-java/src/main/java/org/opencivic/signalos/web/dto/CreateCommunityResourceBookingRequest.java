package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommunityResourceBookingRequest(
    UUID communityId,
    UUID resourceId,
    String purpose,
    LocalDateTime startsAt,
    LocalDateTime endsAt
) {}