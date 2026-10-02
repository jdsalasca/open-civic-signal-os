package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityIntegrationFanOutResponse(
    UUID communityId,
    String eventType,
    int targeted,
    int delivered,
    int failed,
    int skippedNoConnector,
    List<CommunityIntegrationDeliveryResponse> deliveries
) {}