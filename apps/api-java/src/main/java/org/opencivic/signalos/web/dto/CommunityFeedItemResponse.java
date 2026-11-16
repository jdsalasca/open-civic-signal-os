package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record CommunityFeedItemResponse(
    String type,
    UUID id,
    UUID communityId,
    String title,
    String summary,
    LocalDateTime happenedAt,
    String freshness,
    Long confirmationCount,
    Boolean viewerConfirmed,
    Boolean following,
    Map<String, Integer> reactions,
    String viewerReaction,
    String authorName,
    String authorRole,
    Long commentCount
) {}
