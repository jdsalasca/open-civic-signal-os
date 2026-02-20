package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;

public record SignalMetaResponse(
    long totalSignals,
    long unresolvedSignals,
    LocalDateTime lastUpdatedAt
) {}
