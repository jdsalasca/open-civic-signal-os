package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SensitiveDataAccessLogResponse(
    UUID id,
    String accessType,
    UUID actorUserId,
    String actorUsername,
    UUID targetUserId,
    String targetUsername,
    UUID communityId,
    String communityName,
    String note,
    LocalDateTime createdAt
) {}
