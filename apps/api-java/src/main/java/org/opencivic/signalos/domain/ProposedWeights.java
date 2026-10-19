package org.opencivic.signalos.domain;

/**
 * A candidate set of weights for the prioritization formula.
 *
 * <p>Deliberately not {@link PrioritizationFormula}. That class holds the weights actually running,
 * as constants a deploy changes. This is a what-if: a set of numbers a community is considering,
 * which must be evaluable without any chance of it becoming live by accident.
 *
 * <p>Validation lives here rather than in the service because a proposal with a zero or negative
 * multiplier is meaningless in a way that would otherwise surface as a confusing preview: every
 * signal would tie, or scores would go negative, and the preview would look like a legitimate
 * finding.
 */
public record ProposedWeights(
    double urgencyMultiplier,
    double impactMultiplier,
    double affectedPeopleDivisor,
    double affectedPeopleCap,
    double communityVotesDivisor,
    double communityVotesCap
) {

    public ProposedWeights {
        requirePositive("urgencyMultiplier", urgencyMultiplier);
        requirePositive("impactMultiplier", impactMultiplier);
        requirePositive("affectedPeopleDivisor", affectedPeopleDivisor);
        requirePositive("affectedPeopleCap", affectedPeopleCap);
        requirePositive("communityVotesDivisor", communityVotesDivisor);
        requirePositive("communityVotesCap", communityVotesCap);
    }

    /** The weights currently in force, so a proposal always records what it changes from. */
    public static ProposedWeights current() {
        return new ProposedWeights(
            PrioritizationFormula.URGENCY_MULTIPLIER,
            PrioritizationFormula.IMPACT_MULTIPLIER,
            PrioritizationFormula.AFFECTED_PEOPLE_DIVISOR,
            PrioritizationFormula.AFFECTED_PEOPLE_CAP,
            PrioritizationFormula.COMMUNITY_VOTES_DIVISOR,
            PrioritizationFormula.COMMUNITY_VOTES_CAP
        );
    }

    public double urgencyTerm(int urgency) {
        return urgency * urgencyMultiplier;
    }

    public double impactTerm(int impact) {
        return impact * impactMultiplier;
    }

    public double affectedPeopleTerm(int affectedPeople) {
        return Math.min(affectedPeople / affectedPeopleDivisor, affectedPeopleCap);
    }

    public double communityVotesTerm(int communityVotes) {
        return Math.min(communityVotes / communityVotesDivisor, communityVotesCap);
    }

    public double score(int urgency, int impact, int affectedPeople, int communityVotes) {
        return urgencyTerm(urgency)
            + impactTerm(impact)
            + affectedPeopleTerm(affectedPeople)
            + communityVotesTerm(communityVotes);
    }

    /** The expression as text, built from these numbers so a proposal can show what it means. */
    public String expression() {
        return "(Urgency * " + format(urgencyMultiplier) + ")"
            + " + (Impact * " + format(impactMultiplier) + ")"
            + " + min(People/" + format(affectedPeopleDivisor) + ", " + format(affectedPeopleCap) + ")"
            + " + min(Votes/" + format(communityVotesDivisor) + ", " + format(communityVotesCap) + ")";
    }

    private static void requirePositive(String field, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(
                field + " must be a finite number greater than 0, but was: " + value
                    + ". A zero or negative weight would make every signal tie or produce negative scores, "
                    + "which reads as a finding rather than as the invalid input it is.");
        }
    }

    private static String format(double value) {
        return value == Math.rint(value)
            ? String.valueOf((long) value)
            : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}