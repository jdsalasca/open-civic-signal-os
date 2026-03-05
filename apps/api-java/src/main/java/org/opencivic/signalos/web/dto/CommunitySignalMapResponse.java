package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunitySignalMapResponse(
    UUID communityId,
    String communityName,
    String communitySlug,
    LocalDateTime generatedAt,
    String freshness,
    SignalMapFiltersResponse filters,
    List<String> availableCategories,
    List<String> availableStatuses,
    int mappedSignalsCount,
    int unmappedSignalsCount,
    double cumulativeHeatScore,
    List<CommunitySignalMapPointResponse> points,
    List<CommunitySignalClusterResponse> clusters
) {
}
