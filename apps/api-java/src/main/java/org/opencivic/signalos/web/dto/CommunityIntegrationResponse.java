package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityIntegrationResponse(
    UUID id,
    UUID communityId,
    String channel,
    String name,
    String targetUri,
    boolean enabled,
    boolean autoRetry,
    boolean dispatchable,
    UUID createdBy,
    LocalDateTime createdAt,
    LocalDateTime lastAttemptAt,
    LocalDateTime lastSuccessAt,
    int consecutiveFailures,
    long pendingDeliveries,
    long failedDeliveries
) {}