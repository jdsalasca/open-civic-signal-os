package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SignalAgingResponse(
    UUID communityId,
    String generatedAt,
    long slaTargetDays,
    long unresolvedCount,
    long atRiskCount,
    long breachedCount,
    long medianAgeDays,
    List<SignalAgingBucketResponse> ageBuckets,
    List<SignalAgingItemResponse> atRiskSignals,
    List<SignalAgingTrendPointResponse> trend
) {}