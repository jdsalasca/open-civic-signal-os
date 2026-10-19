package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpenDataMetricRecordResponse(
    UUID communityId,
    String period,
    String key,
    String label,
    String value,
    String unit,
    String definition,
    String formula,
    String freshness,
    boolean lowData,
    LocalDateTime generatedAt
) {}
