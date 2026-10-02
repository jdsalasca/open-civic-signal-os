package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityIntegrationCenterResponse(
    UUID communityId,
    String communityName,
    List<String> supportedChannels,
    int integrationCount,
    long pendingDeliveries,
    long failedDeliveries,
    List<CommunityIntegrationResponse> integrations,
    List<CommunityIntegrationDeliveryResponse> recentDeliveries
) {}