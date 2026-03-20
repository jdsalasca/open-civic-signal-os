package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityOpenDataAccessLogResponse(
    UUID id,
    String accessChannel,
    String exportType,
    String format,
    String actorUsername,
    String tokenLabel,
    String note,
    LocalDateTime createdAt
) {}
