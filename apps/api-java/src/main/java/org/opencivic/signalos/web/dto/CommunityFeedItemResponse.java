package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityFeedItemResponse(
    String type,
    UUID id,
    UUID communityId,
    String title,
    String summary,
    LocalDateTime happenedAt,
    String freshness
) {}
