package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunitySignalHeatCellResponse(
    UUID communityId,
    String communityName,
    String communitySlug,
    double latitude,
    double longitude,
    int mappedSignalsCount,
    int unmappedSignalsCount,
    double cumulativeHeatScore,
    double averagePriorityScore,
    String topCategory,
    UUID topSignalId,
    String topSignalTitle
) {
}
