package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityModerationQueueResponse(
    UUID communityId,
    long openReports,
    long actionedReports,
    long dismissedReports,
    long activeSanctions,
    List<CommunityModerationReportResponse> reports
) {}