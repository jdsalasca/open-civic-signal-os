package org.opencivic.signalos.domain;

import java.util.Locale;

/**
 * The prioritization formula, as constants rather than as prose in three places.
 *
 * <p>Before this class the same weights existed in three copies: the arithmetic in
 * {@code PrioritizationServiceImpl}, the published strings in {@code PrioritizationFormulaService},
 * and the human-readable expression in {@code TrustPacket}. A comment in the formula service named
 * the risk and suggested a drift check as the upgrade path.
 *
 * <p>A drift check was the wrong fix. Two copies that a test compares are still two copies, and the
 * test only catches the drift after someone has already changed one of them. There is now one
 * definition, and the other three derive from it, so the published formula cannot disagree with the
 * running one by construction rather than by vigilance.
 *
 * <p>Every published claim about this platform's ranking is ultimately a claim about these numbers.
 * When a formula version changes, this is the file that changes, and the version constant in
 * {@code PrioritizationFormulaService} is the record of that.
 */
public final class PrioritizationFormula {

    /** Urgency is the strongest signal by design: it is the reporter's own assessment of danger. */
    public static final double URGENCY_MULTIPLIER = 30.0;

    public static final double IMPACT_MULTIPLIER = 25.0;

    /** Scale and votes saturate, so a single viral report cannot dominate the backlog. */
    public static final double AFFECTED_PEOPLE_DIVISOR = 10.0;
    public static final double AFFECTED_PEOPLE_CAP = 30.0;
    public static final double COMMUNITY_VOTES_DIVISOR = 5.0;
    public static final double COMMUNITY_VOTES_CAP = 15.0;

    private PrioritizationFormula() {}

    public static double urgencyTerm(int urgency) {
        return urgency * URGENCY_MULTIPLIER;
    }

    public static double impactTerm(int impact) {
        return impact * IMPACT_MULTIPLIER;
    }

    public static double affectedPeopleTerm(int affectedPeople) {
        return Math.min(affectedPeople / AFFECTED_PEOPLE_DIVISOR, AFFECTED_PEOPLE_CAP);
    }

    public static double communityVotesTerm(int communityVotes) {
        return Math.min(communityVotes / COMMUNITY_VOTES_DIVISOR, COMMUNITY_VOTES_CAP);
    }

    public static double score(int urgency, int impact, int affectedPeople, int communityVotes) {
        return urgencyTerm(urgency)
            + impactTerm(impact)
            + affectedPeopleTerm(affectedPeople)
            + communityVotesTerm(communityVotes);
    }

    /**
     * The expression as a string, built from the constants above.
     *
     * <p>Built rather than written out, because a hand-written string is exactly the copy that
     * drifts. Whole numbers render without a decimal point so the published text stays readable.
     */
    public static String expression() {
        return "(Urgency * " + format(URGENCY_MULTIPLIER) + ")"
            + " + (Impact * " + format(IMPACT_MULTIPLIER) + ")"
            + " + min(People/" + format(AFFECTED_PEOPLE_DIVISOR) + ", " + format(AFFECTED_PEOPLE_CAP) + ")"
            + " + min(Votes/" + format(COMMUNITY_VOTES_DIVISOR) + ", " + format(COMMUNITY_VOTES_CAP) + ")";
    }

    private static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}