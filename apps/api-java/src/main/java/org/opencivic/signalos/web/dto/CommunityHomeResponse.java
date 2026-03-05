package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityHomeResponse(
    UUID communityId,
    LocalDateTime generatedAt,
    String freshness,
    int activeRoomsCount,
    List<CommunityBlogPostResponse> officialUpdates,
    List<CommunityThreadResponse> hotThreads,
    List<CommunityHomeSignalResponse> topSignals
) {}
