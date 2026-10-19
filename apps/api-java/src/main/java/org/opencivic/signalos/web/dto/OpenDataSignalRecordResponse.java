package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpenDataSignalRecordResponse(
    UUID id,
    UUID communityId,
    String title,
    String category,
    String status,
    double priorityScore,
    int urgency,
    int impact,
    int affectedPeople,
    int communityVotes,
    String locationLabel,
    LocalDateTime createdAt
) {}
