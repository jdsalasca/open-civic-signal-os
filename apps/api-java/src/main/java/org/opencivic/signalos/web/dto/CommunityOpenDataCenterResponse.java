package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityOpenDataCenterResponse(
    UUID communityId,
    String communityName,
    int defaultRateLimitPerHour,
    List<CommunityOpenDataDatasetResponse> datasets,
    List<CommunityOpenDataTokenResponse> tokens,
    List<CommunityOpenDataAccessLogResponse> recentAccessLogs
) {}
