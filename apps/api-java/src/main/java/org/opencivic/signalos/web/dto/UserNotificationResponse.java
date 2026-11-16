package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserNotificationResponse(
    UUID id,
    String type,
    String actorName,
    String targetType,
    UUID targetId,
    String targetTitle,
    UUID communityId,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
