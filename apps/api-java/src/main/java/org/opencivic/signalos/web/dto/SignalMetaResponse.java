package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;

public record SignalMetaResponse(
    long totalSignals,
    long unresolvedSignals,
    LocalDateTime lastUpdatedAt,
    /**
     * Default score at or above which a signal counts as critical.
     *
     * <p>Published rather than hardcoded in the dashboard so the client filter and the backend
     * filter cannot drift apart, and so a resident can see what number is being used.
     */
    double criticalScoreThreshold
) {}
