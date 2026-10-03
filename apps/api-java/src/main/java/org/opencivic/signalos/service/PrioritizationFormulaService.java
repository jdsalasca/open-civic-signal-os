package org.opencivic.signalos.service;

import java.util.List;
import org.opencivic.signalos.domain.PrioritizationFormula;
import org.opencivic.signalos.web.dto.FormulaWeightResponse;
import org.opencivic.signalos.web.dto.PrioritizationFormulaResponse;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for what the prioritization formula is.
 *
 * <p>The weights used to be hardcoded here as prose while the arithmetic lived in
 * {@link PrioritizationServiceImpl}, which is two copies of the same numbers and a standing risk of
 * the published formula disagreeing with the running one. They now come from
 * {@link PrioritizationFormula}, so this class renders the rule rather than restating it.
 */
@Service
public class PrioritizationFormulaService {
    public static final String VERSION = "v1";
    static final String EFFECTIVE_FROM = "2026-02-19";
    static final String CHANGE_NOTE =
        "Initial published weighting: urgency 30, impact 25, affected people capped at 30, "
            + "community votes capped at 15.";

    private static List<FormulaWeightResponse> weights() {
        return List.of(
            new FormulaWeightResponse(
                "urgency", "urgency (1-5)",
                "urgency * " + trimmed(PrioritizationFormula.URGENCY_MULTIPLIER),
                PrioritizationFormula.urgencyTerm(5)),
            new FormulaWeightResponse(
                "impact", "impact (1-5)",
                "impact * " + trimmed(PrioritizationFormula.IMPACT_MULTIPLIER),
                PrioritizationFormula.impactTerm(5)),
            new FormulaWeightResponse(
                "affectedPeople", "affectedPeople (citizens)",
                "min(affectedPeople / " + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_DIVISOR)
                    + ", " + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_CAP) + ")",
                PrioritizationFormula.AFFECTED_PEOPLE_CAP),
            new FormulaWeightResponse(
                "communityVotes", "communityVotes",
                "min(communityVotes / " + trimmed(PrioritizationFormula.COMMUNITY_VOTES_DIVISOR)
                    + ", " + trimmed(PrioritizationFormula.COMMUNITY_VOTES_CAP) + ")",
                PrioritizationFormula.COMMUNITY_VOTES_CAP)
        );
    }

    private static String trimmed(double value) {
        return value == Math.rint(value)
            ? String.valueOf((long) value)
            : String.valueOf(value);
    }

    public PrioritizationFormulaResponse getFormula() {
        return new PrioritizationFormulaResponse(
            VERSION,
            PrioritizationFormula.expression(),
            EFFECTIVE_FROM,
            weights(),
            List.of("affectedPeople", "communityVotes"),
            CHANGE_NOTE
        );
    }
}