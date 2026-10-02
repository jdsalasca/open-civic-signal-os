package org.opencivic.signalos.service;

import java.util.List;
import org.opencivic.signalos.web.dto.FormulaWeightResponse;
import org.opencivic.signalos.web.dto.PrioritizationFormulaResponse;
import org.opencivic.signalos.web.dto.TrustPacket;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for what the prioritization formula is.
 *
 * ponytail: the weights are still hardcoded in {@link PrioritizationServiceImpl#getBreakdown};
 * this class mirrors them for publication. A drift check between the two is the honest
 * upgrade path, and the ADRs note it as open. Do not edit one side only.
 */
@Service
public class PrioritizationFormulaService {
    public static final String VERSION = "v1";
    static final String EFFECTIVE_FROM = "2026-02-19";
    static final String CHANGE_NOTE =
        "Initial published weighting: urgency 30, impact 25, affected people capped at 30, "
            + "community votes capped at 15.";

    private static final List<FormulaWeightResponse> WEIGHTS = List.of(
        new FormulaWeightResponse("urgency", "urgency (1-5)", "urgency * 30", 150.0),
        new FormulaWeightResponse("impact", "impact (1-5)", "impact * 25", 125.0),
        new FormulaWeightResponse(
            "affectedPeople", "affectedPeople (citizens)", "min(affectedPeople / 10, 30)", 30.0
        ),
        new FormulaWeightResponse("communityVotes", "communityVotes", "min(communityVotes / 5, 15)", 15.0)
    );

    public PrioritizationFormulaResponse getFormula() {
        return new PrioritizationFormulaResponse(
            VERSION,
            TrustPacket.CURRENT_FORMULA,
            EFFECTIVE_FROM,
            WEIGHTS,
            List.of("affectedPeople", "communityVotes"),
            CHANGE_NOTE
        );
    }
}