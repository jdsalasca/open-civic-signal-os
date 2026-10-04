package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One month's transparency figures.
 *
 * <p><b>Reproducible in its counts, not in its scores.</b> Every count, status and list membership is
 * derived from the audit trail bounded by the period end, so regenerating the same month later yields
 * the same figures. {@code priorityScore} is the exception: it is mutable and has no history, so the
 * score attached to a past period is today's score.
 *
 * <p>This record used to claim flatly that "everything is derived from the stored period bounds, so
 * regenerating the same month later yields the same figures". That was false for the scores, and a
 * consumer told a report is reproducible will reasonably rely on it. So the gap is stated in the payload
 * as {@code reproducibilityLimits} rather than only here in the javadoc, where a consumer integrating
 * against the JSON would never read it.
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
    /** What in this report cannot be reproduced from stored history. Never empty while scores have no ledger. */
    List<String> reproducibilityLimits,
    LocalDateTime generatedAt
) {
    public boolean complete() {
        return period != null && period.previous() != null && metrics != null;
    }
}