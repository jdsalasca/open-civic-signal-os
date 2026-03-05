package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunitySignalClusterResponse(
    String clusterKey,
    UUID communityId,
    String communityName,
    double latitude,
    double longitude,
    int signalCount,
    double cumulativePriorityScore,
    String primaryCategory,
    UUID topSignalId,
    String topSignalTitle
) {
}
