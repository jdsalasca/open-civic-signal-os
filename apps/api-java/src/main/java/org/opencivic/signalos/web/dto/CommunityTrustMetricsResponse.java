package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityTrustMetricsResponse(
    UUID communityId,
    String communityName,
    String communitySlug,
    String period,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime generatedAt,
    LocalDateTime lastUpdatedAt,
    String freshness,
    boolean lowData,
    String lowDataReason,
    List<TrustMetricCardResponse> cards,
    List<TrustMetricBreakdownResponse> breakdowns
) {}
