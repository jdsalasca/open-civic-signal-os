package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.opencivic.signalos.domain.SignalMergeDecision;

/**
 * The outcome of a recorded review decision.
 *
 * <p>{@code mergedTargetSignalId} is null for anything other than an approval, so a caller cannot
 * mistake "we recorded your rejection" for "we merged them".
 */
public record SignalMergeReviewResponse(
    UUID decisionId,
    UUID targetSignalId,
    String targetTitle,
    SignalMergeDecision.Decision decision,
    int suggestedCount,
    UUID mergedTargetSignalId,
    String mergedTitle,
    LocalDateTime decidedAt,
    String note
) {}