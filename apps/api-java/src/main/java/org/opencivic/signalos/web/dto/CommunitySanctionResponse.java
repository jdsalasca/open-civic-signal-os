package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunitySanctionResponse(
    UUID id,
    String sanctionType,
    String status,
    String reason,
    UUID targetUserId,
    String targetUsername,
    UUID issuedByUserId,
    String issuedByUsername,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    boolean appealAvailable
) {}