package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommunityActivityRequest(
    UUID communityId,
    String title,
    String description,
    String locationLabel,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    LocalDateTime signupOpensAt,
    LocalDateTime signupClosesAt,
    Integer signupCapacity
) {}