package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityIntegrationDeliveryResponse(
    UUID id,
    UUID integrationId,
    String integrationName,
    String channel,
    String eventType,
    UUID referenceId,
    String status,
    int attempts,
    String lastError,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}