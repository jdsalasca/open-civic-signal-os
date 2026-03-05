package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SignalTimelineEntryResponse(
    UUID id,
    UUID signalId,
    String eventType,
    String statusFrom,
    String statusTo,
    String changedBy,
    String assignedToUsername,
    String reason,
    LocalDateTime createdAt
) {}
