package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunitySignalMapPointResponse(
    UUID signalId,
    UUID communityId,
    String communityName,
    String title,
    String category,
    String status,
    String locationLabel,
    double latitude,
    double longitude,
    double priorityScore,
    double heatWeight,
    LocalDateTime createdAt
) {
}
