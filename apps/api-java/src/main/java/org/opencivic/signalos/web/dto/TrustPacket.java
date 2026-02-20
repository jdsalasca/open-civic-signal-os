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
    public static final String CURRENT_FORMULA = "(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)";
}
