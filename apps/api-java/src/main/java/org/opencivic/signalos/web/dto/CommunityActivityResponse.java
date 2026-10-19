package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityActivityResponse(
    UUID id,
    UUID communityId,
    String title,
    String description,
    String locationLabel,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    LocalDateTime signupOpensAt,
    LocalDateTime signupClosesAt,
    int signupCapacity,
    UUID organizerId,
    String organizerName,
    LocalDateTime createdAt,
    boolean cancelled,
    String signupWindowState,
    boolean signupOpen,
    String fullReason,
    long confirmedCount,
    long fillRatePercent,
    UUID currentVolunteerSignupId,
    String currentVolunteerStatus,
    List<CommunityActivityVolunteerResponse> roster
) {}