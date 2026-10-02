package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityActivityVolunteerResponse(
    UUID signupId,
    UUID volunteerId,
    String volunteerName,
    String status,
    String attendanceStatus,
    LocalDateTime createdAt,
    LocalDateTime cancelledAt,
    LocalDateTime attendedAt
) {}