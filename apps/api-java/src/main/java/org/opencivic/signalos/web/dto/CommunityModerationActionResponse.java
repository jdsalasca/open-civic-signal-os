package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;

public record CommunityModerationActionResponse(
    String actionType,
    String actorUsername,
    String note,
    LocalDateTime happenedAt
) {}