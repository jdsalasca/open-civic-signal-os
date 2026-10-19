package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunitySignalsHeatMapResponse(
    LocalDateTime generatedAt,
    String freshness,
    SignalMapFiltersResponse filters,
    List<String> availableCategories,
    List<String> availableStatuses,
    int visibleCommunitiesCount,
    int totalMappedSignalsCount,
    double totalHeatScore,
    List<CommunitySignalHeatCellResponse> communities
) {
}
