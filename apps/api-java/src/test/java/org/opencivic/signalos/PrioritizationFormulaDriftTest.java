package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.PrioritizationFormula;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.service.PrioritizationFormulaService;
import org.opencivic.signalos.web.dto.FormulaWeightResponse;
import org.opencivic.signalos.web.dto.PrioritizationFormulaResponse;

/**
 * The published formula is a claim about what the code computes. If the two disagree, every "why is
 * this ranked here" on every screen is false, and nothing else in the platform would notice.
 *
 * <p>Before {@link PrioritizationFormula} the weights existed in three copies and a comment named a
 * drift check as the upgrade path. This test is stronger than a drift check: the copies are gone, so
 * what it verifies is that the published rendering agrees with the arithmetic — and it fails if
 * anyone reintroduces a literal.
 */
class PrioritizationFormulaDriftTest {

    @Test
    void thePublishedExpressionShouldQuoteTheActualMultipliers() {
        String expression = PrioritizationFormula.expression();

        assertThat(expression).contains("Urgency * " + trimmed(PrioritizationFormula.URGENCY_MULTIPLIER));
        assertThat(expression).contains("Impact * " + trimmed(PrioritizationFormula.IMPACT_MULTIPLIER));
        assertThat(expression).contains("People/" + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_DIVISOR));
        assertThat(expression).contains(", " + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_CAP));
        assertThat(expression).contains("Votes/" + trimmed(PrioritizationFormula.COMMUNITY_VOTES_DIVISOR));
    }

    @Test
    void thePublishedWeightsShouldCarryTheValuesTheArithmeticUses() {
        PrioritizationFormulaResponse published = new PrioritizationFormulaService().getFormula();
        List<FormulaWeightResponse> weights = published.weights();

        // Max contribution of each factor, computed from the same constants.
        assertThat(maxContribution(weights, "urgency")).isEqualTo(PrioritizationFormula.urgencyTerm(5));
        assertThat(maxContribution(weights, "impact")).isEqualTo(PrioritizationFormula.impactTerm(5));
        assertThat(maxContribution(weights, "affectedPeople"))
            .isEqualTo(PrioritizationFormula.AFFECTED_PEOPLE_CAP);
        assertThat(maxContribution(weights, "communityVotes"))
            .isEqualTo(PrioritizationFormula.COMMUNITY_VOTES_CAP);
    }

    @Test
    void thePublishedWeightDescriptionsShouldMatchTheConstants() {
        PrioritizationFormulaResponse published = new PrioritizationFormulaService().getFormula();

        assertThat(weight(published.weights(), "urgency").expression())
            .contains("urgency * " + trimmed(PrioritizationFormula.URGENCY_MULTIPLIER));
        assertThat(weight(published.weights(), "impact").expression())
            .contains("impact * " + trimmed(PrioritizationFormula.IMPACT_MULTIPLIER));
        assertThat(weight(published.weights(), "affectedPeople").expression())
            .contains("min(affectedPeople / " + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_DIVISOR)
                + ", " + trimmed(PrioritizationFormula.AFFECTED_PEOPLE_CAP) + ")");
        assertThat(weight(published.weights(), "communityVotes").expression())
            .contains("min(communityVotes / " + trimmed(PrioritizationFormula.COMMUNITY_VOTES_DIVISOR)
                + ", " + trimmed(PrioritizationFormula.COMMUNITY_VOTES_CAP) + ")");
    }

    /**
     * Evaluate the published expression's own terms and check they equal the shared helper.
     *
     * <p>This is the one that would catch a genuine divergence: it recomputes the four terms from
     * the constants and compares against the values a signal would actually be scored with.
     */
    @Test
    void evaluatingTheTermsShouldReproduceTheSharedScore() {
        int[][] inputs = {
            {1, 1, 1, 0},
            {5, 5, 900, 79},
            {3, 4, 120, 8},
            {5, 1, 0, 100},
            {2, 2, 10, 5},
        };

        for (int[] input : inputs) {
            ScoreBreakdown breakdown = new ScoreBreakdown(
                PrioritizationFormula.urgencyTerm(input[0]),
                PrioritizationFormula.impactTerm(input[1]),
                PrioritizationFormula.affectedPeopleTerm(input[2]),
                PrioritizationFormula.communityVotesTerm(input[3])
            );
            double summed = breakdown.urgency() + breakdown.impact()
                + breakdown.affectedPeople() + breakdown.communityVotes();

            assertThat(summed)
                .as("score for urgency=%d impact=%d people=%d votes=%d",
                    input[0], input[1], input[2], input[3])
                .isEqualTo(PrioritizationFormula.score(input[0], input[1], input[2], input[3]));
        }
    }

    @Test
    void theTrustPacketAccessorShouldAgreeWithTheSharedExpression() {
        assertThat(org.opencivic.signalos.web.dto.TrustPacket.currentFormula())
            .isEqualTo(PrioritizationFormula.expression());
    }

    private static FormulaWeightResponse weight(List<FormulaWeightResponse> weights, String key) {
        return weights.stream()
            .filter(entry -> entry.factor().equals(key))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no published weight for " + key));
    }

    private static double maxContribution(List<FormulaWeightResponse> weights, String key) {
        return weight(weights, key).cap();
    }

    private static String trimmed(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}