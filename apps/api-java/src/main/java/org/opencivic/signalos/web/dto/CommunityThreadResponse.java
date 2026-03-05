package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityThreadResponse(
    UUID id,
    UUID sourceCommunityId,
    UUID targetCommunityId,
    UUID relatedSignalId,
    String title,
    UUID createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CommunityThreadMessageResponse> messages
) {}
