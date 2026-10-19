package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityIntegrationResponse(
    UUID id,
    UUID communityId,
    String channel,
    String name,
    String targetUri,
    /**
     * The sending identity a provider needs in its path, currently the WhatsApp phone-number id.
     * Exposed because a coordinator needs to see it is set: a WhatsApp integration without it fails
     * only at delivery time. Neither the secret hash nor the encrypted token is ever exposed.
     */
    String providerResourceId,
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