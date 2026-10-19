package org.opencivic.signalos.web.dto;

import org.opencivic.signalos.domain.ScoreBreakdown;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrustPacket(
    UUID signalId,
    String title,
    String status,
    LocalDateTime createdAt,
    double finalScore,
    ScoreBreakdown scoreBreakdown,
    String prioritizationFormula,
    String verificationHash
) {
    /**
     * The formula as published, derived from the same constants the score is computed with.
     *
     * <p>Previously a hand-written string in this record, which was the third copy of the weights
     * and free to drift from the arithmetic. Kept as a convenience accessor; the definition lives in
     * {@link org.opencivic.signalos.domain.PrioritizationFormula}.
     */
    public static String currentFormula() {
        return org.opencivic.signalos.domain.PrioritizationFormula.expression();
    }
}
