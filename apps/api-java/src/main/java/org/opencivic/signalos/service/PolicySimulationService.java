package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.ProposedWeights;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Policy simulation sandbox: compare several weight sets against the real backlog at once.
 *
 * <p>{@link FormulaChangeProposalService} answers "what would this one change do". A sandbox answers
 * the question that actually precedes a decision: <b>who wins and who loses</b>. A change that moves
 * forty percent of the backlog is not necessarily a problem; a change that moves forty percent of
 * the backlog <em>in one direction for one category</em> is a policy choice, and it should be visible
 * as one before anyone votes on it.
 *
 * <p>So the unit of output is not "positions moved" but <b>per-category gain and loss</b>. That is
 * the difference between a diff and a simulation.
 *
 * <p>Three things this deliberately does not do:
 *
 * <ul>
 *   <li><b>It does not rank the scenarios.</b> There is no "best" weight set, because "best" depends
 *       on what a community wants to prioritise, which is a value judgement the platform has no
 *       standing to make. It reports the consequences and stops.
 *   <li><b>It does not predict the future.</b> Changing the weights changes what people report and
 *       vote on, so the backlog under a new formula would not be this one. Every scenario says so.
 *   <li><b>It does not persist anything.</b> A simulation is a question. Storing it would make it
 *       look like a decision.
 * </ul>
 */
@Service
public class PolicySimulationService {

    public static final String VERSION = "v1";

    private static final int MAX_SCENARIOS = 5;
    private static final int MAX_SIGNALS = 2000;
    private static final int MAX_MOVERS_PER_SCENARIO = 5;
    private static final Set<String> CLOSED_STATUSES = Set.of("RESOLVED", "CLOSED", "REJECTED");

    private final SignalRepository signalRepository;

    public PolicySimulationService(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    public record Scenario(String name, ProposedWeights weights) {}

    /** How one category fares under a scenario, in aggregate. */
    public record CategoryShift(
        String category,
        int signals,
        int gained,
        int lost,
        int netPositionChange,
        double averagePositionDelta
    ) {}

    public record ScenarioResult(
        String name,
        String expression,
        int positionsMoved,
        double movedShare,
        List<CategoryShift> categoryShifts,
        List<FormulaChangeProposalService.Mover> biggestMovers,
        String reading
    ) {}

    public record SimulationReport(
        String version,
        int sampleSize,
        String currentExpression,
        List<ScenarioResult> scenarios,
        String interpretation,
        LocalDateTime computedAt
    ) {}

    /**
     * Runs every scenario against the same backlog, so the comparison is like for like.
     *
     * <p>One snapshot of the backlog is taken and reused. Re-reading per scenario would let a
     * concurrent report change the sample between scenarios, which would make the comparison
     * meaningless in a way nobody could see.
     */
    @Transactional(readOnly = true)
    public SimulationReport simulate(List<Scenario> scenarios, Integer limit) {
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("At least one scenario is required.");
        }
        if (scenarios.size() > MAX_SCENARIOS) {
            throw new IllegalArgumentException(
                "At most " + MAX_SCENARIOS + " scenarios can be compared at once, but got: " + scenarios.size()
                    + ". More than that is a spreadsheet, not a decision aid.");
        }
        for (Scenario scenario : scenarios) {
            if (scenario.name() == null || scenario.name().isBlank()) {
                throw new IllegalArgumentException(
                    "Every scenario needs a name. An unnamed scenario cannot be referred to in the "
                        + "discussion it is meant to inform.");
            }
        }

        List<Signal> backlog = currentBacklog(limit);
        Map<UUID, Integer> currentPositions = positions(backlog, ProposedWeights.current());

        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            results.add(runScenario(scenario, backlog, currentPositions));
        }

        return new SimulationReport(
            VERSION,
            backlog.size(),
            ProposedWeights.current().expression(),
            results,
            interpretation(backlog.size(), scenarios.size()),
            LocalDateTime.now()
        );
    }

    private ScenarioResult runScenario(
        Scenario scenario,
        List<Signal> backlog,
        Map<UUID, Integer> currentPositions
    ) {
        Map<UUID, Integer> proposedPositions = positions(backlog, scenario.weights());

        List<FormulaChangeProposalService.Mover> movers = new ArrayList<>();
        Map<String, int[]> perCategory = new LinkedHashMap<>();
        Map<String, Long> deltaSums = new LinkedHashMap<>();

        for (Signal signal : backlog) {
            Integer before = currentPositions.get(signal.getId());
            Integer after = proposedPositions.get(signal.getId());
            if (before == null || after == null) {
                continue;
            }
            String category = signal.getCategory() == null || signal.getCategory().isBlank()
                ? "unspecified"
                : signal.getCategory().trim().toLowerCase(Locale.ROOT);
            int[] counts = perCategory.computeIfAbsent(category, ignored -> new int[3]);
            counts[0]++; // signals in this category
            int delta = after - before;
            if (delta < 0) {
                counts[1]++; // gained: moved up
            } else if (delta > 0) {
                counts[2]++; // lost: moved down
            }
            deltaSums.merge(category, (long) delta, Long::sum);

            if (delta != 0) {
                movers.add(new FormulaChangeProposalService.Mover(
                    signal.getId(), signal.getTitle(), before, after, delta));
            }
        }

        movers.sort(Comparator
            .comparingInt((FormulaChangeProposalService.Mover mover) -> Math.abs(mover.positionDelta()))
            .reversed()
            .thenComparing(FormulaChangeProposalService.Mover::signalId));

        List<CategoryShift> shifts = new ArrayList<>();
        perCategory.forEach((category, counts) -> shifts.add(new CategoryShift(
            category,
            counts[0],
            counts[1],
            counts[2],
            // Net: positive means the category moved up overall, negative means down.
            -deltaSums.getOrDefault(category, 0L).intValue(),
            counts[0] == 0 ? 0.0 : round2(-(double) deltaSums.getOrDefault(category, 0L) / counts[0])
        )));
        // Biggest absolute shift first, so the category most affected is the one read first.
        shifts.sort(Comparator
            .comparingDouble((CategoryShift shift) -> Math.abs(shift.averagePositionDelta()))
            .reversed()
            .thenComparing(CategoryShift::category));

        int moved = movers.size();
        double movedShare = backlog.isEmpty() ? 0.0 : round2((double) moved / backlog.size());

        return new ScenarioResult(
            scenario.name().trim(),
            scenario.weights().expression(),
            moved,
            movedShare,
            shifts,
            movers.stream().limit(MAX_MOVERS_PER_SCENARIO).toList(),
            reading(scenario.name().trim(), shifts, movedShare)
        );
    }

    /**
     * Names the category that gains most and the one that loses most, because that is the finding.
     *
     * <p>"38% of the backlog moves" is a diff. "Utilities gains an average of four places while
     * infrastructure loses three" is a policy consequence, and it is the sentence a decision should
     * be argued about.
     */
    private String reading(String name, List<CategoryShift> shifts, double movedShare) {
        if (shifts.isEmpty()) {
            return name + ": no categories to compare.";
        }
        CategoryShift biggestGain = shifts.stream()
            .max(Comparator.comparingDouble(CategoryShift::averagePositionDelta))
            .orElse(null);
        CategoryShift biggestLoss = shifts.stream()
            .min(Comparator.comparingDouble(CategoryShift::averagePositionDelta))
            .orElse(null);

        StringBuilder text = new StringBuilder();
        text.append(name).append(": ").append(Math.round(movedShare * 100))
            .append("% of the backlog changes position.");
        if (biggestGain != null && biggestGain.averagePositionDelta() > 0) {
            text.append(" ").append(biggestGain.category()).append(" gains an average of ")
                .append(biggestGain.averagePositionDelta()).append(" place(s).");
        }
        if (biggestLoss != null && biggestLoss.averagePositionDelta() < 0) {
            text.append(" ").append(biggestLoss.category()).append(" loses an average of ")
                .append(Math.abs(biggestLoss.averagePositionDelta())).append(" place(s).");
        }
        if (biggestGain != null && biggestLoss != null
            && biggestGain.category().equals(biggestLoss.category())) {
            text.append(" Only one category is materially affected, so this is close to a targeted change.");
        }
        return text.toString();
    }

    private String interpretation(int sampleSize, int scenarioCount) {
        return "This compares " + scenarioCount + " weight set(s) against the same " + sampleSize
            + " signals that exist now, and reports which categories gain and lose. It does NOT rank "
            + "the scenarios: there is no best weight set, because best depends on what a community "
            + "wants to prioritise, which is a value judgement the platform has no standing to make. "
            + "It also does not predict the future: changing how signals are scored changes what "
            + "people report and vote on, so the backlog under any of these would not be this one. "
            + "Nothing here is stored; a simulation is a question, not a decision.";
    }

    private Map<UUID, Integer> positions(List<Signal> backlog, ProposedWeights weights) {
        List<UUID> ordered = backlog.stream()
            .sorted(Comparator
                .comparingDouble((Signal signal) -> weights.score(
                    signal.getUrgency(),
                    signal.getImpact(),
                    signal.getAffectedPeople(),
                    signal.getCommunityVotes()))
                .reversed()
                .thenComparing(Signal::getId))
            .map(Signal::getId)
            .toList();
        Map<UUID, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            positions.put(ordered.get(i), i + 1);
        }
        return positions;
    }

    private List<Signal> currentBacklog(Integer limit) {
        int effective = limit == null ? MAX_SIGNALS : limit;
        if (effective < 1 || effective > MAX_SIGNALS) {
            throw new IllegalArgumentException(
                "limit must be between 1 and " + MAX_SIGNALS + ", but was: " + limit);
        }
        return signalRepository.findAll().stream()
            .filter(signal -> signal.getStatus() == null
                || !CLOSED_STATUSES.contains(signal.getStatus().toUpperCase(Locale.ROOT)))
            .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed()
                .thenComparing(Signal::getId))
            .limit(effective)
            .toList();
    }

    private double round2(double value) {
        return java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .doubleValue();
    }
}