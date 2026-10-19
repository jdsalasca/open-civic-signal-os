package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublishCommunityIntegrationEventRequest(
    UUID communityId,
    String eventType,
    UUID referenceId,
    String title,
    String description,
    String locationLabel,
    LocalDateTime startsAt,
    LocalDateTime endsAt
) {}