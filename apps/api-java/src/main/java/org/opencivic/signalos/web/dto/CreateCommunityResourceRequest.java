package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommunityResourceRequest(
    UUID communityId,
    String name,
    String description,
    String locationLabel,
    Boolean requiresApproval,
    Integer minNoticeHours,
    Integer maxBookingHours
) {}