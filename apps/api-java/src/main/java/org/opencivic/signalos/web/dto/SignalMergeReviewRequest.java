package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.SignalMergeDecision;

/**
 * A reviewer's decision about one proposed merge.
 *
 * <p>{@code suggestedSimilarities} is supplied by the client rather than recomputed server-side on
 * purpose: the record has to capture what the person was actually shown. Recomputing it here would
 * store today's number and quietly rewrite history if the algorithm changes.
 */
public record SignalMergeReviewRequest(
    @NotNull UUID targetSignalId,
    @NotNull SignalMergeDecision.Decision decision,
    @NotNull List<SignalMergeSuggestionResponse.SuggestedDuplicate> suggestedSimilarities,
    double threshold,
    String note
) {}