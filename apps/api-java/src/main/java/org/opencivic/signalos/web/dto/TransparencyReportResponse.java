package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Monthly transparency report for one community over one closed calendar month.
 *
 * <p>Deterministic by construction: everything is derived from the stored period bounds, so
 * regenerating the same month later yields the same figures. Nothing here reads "now" except
 * {@code generatedAt}, which is metadata about the run rather than about the community.
 */
public record TransparencyReportResponse(
    UUID communityId,
    String communityName,
    TransparencyPeriod period,
    List<TransparencyMetricResponse> metrics,
    List<TransparencySignalOutcomeResponse> actioned,
    List<TransparencySignalOutcomeResponse> unaddressed,
    List<String> narrative,
    String formulaVersion,
    LocalDateTime generatedAt
) {
    public boolean complete() {
        return period != null && period.previous() != null && metrics != null;
    }
}