package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityResourceResponse(
    UUID id,
    UUID communityId,
    String name,
    String description,
    String locationLabel,
    boolean requiresApproval,
    int minNoticeHours,
    int maxBookingHours,
    boolean archived,
    UUID createdBy,
    LocalDateTime createdAt,
    long upcomingBlockingCount,
    UUID currentRequesterBookingId,
    String currentRequesterBookingStatus,
    List<CommunityResourceBookingResponse> upcomingBookings
) {}