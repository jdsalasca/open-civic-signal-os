package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One duplicate the algorithm proposes, with the score that justified proposing it.
 *
 * <p>The score travels with the suggestion because a reviewer deciding whether two reports are
 * the same complaint needs to see why the platform thinks they are.
 */
public record SignalMergeSuggestionResponse(
    UUID targetSignalId,
    String targetTitle,
    String targetCategory,
    java.util.List<SignalMergeSuggestionResponse.SuggestedDuplicate> candidates,
    double threshold,
    String latestDecision,
    LocalDateTime reviewedAt
) {
    public record SuggestedDuplicate(
        UUID signalId,
        String title,
        String category,
        String status,
        LocalDateTime reportedAt,
        /** 0..1. Null when the caller already supplied the pair without a score. */
        Double similarity
    ) {}
}