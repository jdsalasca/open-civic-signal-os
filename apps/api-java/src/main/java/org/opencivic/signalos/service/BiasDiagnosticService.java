package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bias diagnostics: does the platform treat comparable reports unequally?
 *
 * <p>The issue asks for fairness weighting. This deliberately does not implement one, and the
 * reason is the whole design.
 *
 * <p>Weighting a ranking by equity requires equity data: who lives where, which areas are
 * underserved, which populations are under-reporting. This platform has none of that. An "equity
 * multiplier" built on absent data would be a number invented to look fair, and it would move real
 * priorities invisibly. That is precisely the black-box governance the project forbids.
 *
 * <p>What the platform can do honestly is measure whether it treats comparable reports
 * inconsistently, using data it actually has. If reports about roads wait four times as long as
 * reports about parks, that is a measurable, explainable, and fixable disparity, and nobody had to
 * invent a demographic weight to see it.
 *
 * <p>What this does <b>not</b> measure is fairness across populations. It measures consistency of
 * treatment between categories. The distinction is stated in the response rather than left for a
 * reader to assume, because a report titled "bias diagnostics" that stayed quiet about its own
 * limits would be its own kind of dishonesty.
 *
 * <p>Disparity is measured against the community's own baseline, not a national standard, and a
 * category needs enough reports before any comparison is drawn. Two reports averaging 40 days is
 * noise, and flagging it would train people to ignore the flags.
 */
@Service
public class BiasDiagnosticService {

    public static final String VERSION = "v1";

    /** Below this many reports, a category has no baseline worth comparing. */
    private static final int MIN_CATEGORY_REPORTS = 3;
    /** Below this many reports overall, the community itself has no baseline. */
    private static final int MIN_COMMUNITY_REPORTS = 8;
    /** How much slower than the community median before resolution time is called disparate. */
    private static final double SLOWER_RESOLUTION_MULTIPLIER = 2.0;
    /** How far below the community resolved share before a category is called under-resolved. */
    private static final double LOWER_RESOLUTION_RATE_MARGIN = 0.30;
    /** How far below the community attention rate before a category is called under-attended. */
    private static final double LOWER_ATTENTION_MULTIPLIER = 0.5;

    private static final Set<String> CLOSED_STATUSES = Set.of("RESOLVED", "CLOSED", "REJECTED");
    private static final Set<String> EXCLUDED_STATUSES = Set.of("FLAGGED");

    public record Thresholds(
        int minCategoryReports,
        int minCommunityReports,
        double slowerResolutionMultiplier,
        double lowerResolutionRateMargin,
        double lowerAttentionMultiplier
    ) {}

    /** One category's measured treatment, with every number the verdict came from. */
    public record CategoryDiagnostic(
        String category,
        int reports,
        int resolved,
        long medianDaysToResolve,
        double resolvedShare,
        double medianPriorityScore,
        double votesPerReport,
        List<String> flags
    ) {}

    public record BiasDiagnosticReport(
        String version,
        String communityId,
        LocalDateTime generatedAt,
        Thresholds thresholds,
        int totalReportsConsidered,
        long communityMedianDaysToResolve,
        double communityResolvedShare,
        double communityVotesPerReport,
        /** The best-treated category with enough reports, which every comparison is drawn against. */
        String referenceCategory,
        List<CategoryDiagnostic> categories,
        List<String> disparateCategories,
        int categoriesWithoutBaseline,
        String scopeStatement
    ) {}

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository statusEntryRepository;

    public BiasDiagnosticService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository statusEntryRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.statusEntryRepository = statusEntryRepository;
    }

    public Thresholds defaultThresholds() {
        return new Thresholds(
            MIN_CATEGORY_REPORTS,
            MIN_COMMUNITY_REPORTS,
            SLOWER_RESOLUTION_MULTIPLIER,
            LOWER_RESOLUTION_RATE_MARGIN,
            LOWER_ATTENTION_MULTIPLIER
        );
    }

    @Transactional(readOnly = true)
    public BiasDiagnosticReport analyse(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        if (!communityRepository.existsById(communityId)) {
            throw new ResourceNotFoundException("Community not found: " + communityId);
        }
        return buildReport(communityId);
    }

    @Transactional(readOnly = true)
    public BiasDiagnosticReport buildReport(UUID communityId) {
        List<Signal> signals = signalRepository.findByCommunityId(communityId).stream()
            .filter(signal -> signal.getStatus() == null
                || !EXCLUDED_STATUSES.contains(signal.getStatus().toUpperCase(Locale.ROOT)))
            .toList();

        Map<UUID, List<SignalStatusEntry>> history = signals.isEmpty()
            ? Map.of()
            : statusEntryRepository
                .findBySignalIdInOrderByCreatedAtAsc(signals.stream().map(Signal::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(SignalStatusEntry::getSignalId));

        Map<String, List<Signal>> byCategory = new LinkedHashMap<>();
        for (Signal signal : signals) {
            String category = signal.getCategory() == null || signal.getCategory().isBlank()
                ? "unspecified"
                : signal.getCategory().trim().toLowerCase(Locale.ROOT);
            byCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(signal);
        }

        Thresholds thresholds = defaultThresholds();

        // Build every category's measurements first, so the reference can be chosen from them.
        List<CategoryDiagnostic> measured = new ArrayList<>();
        for (Map.Entry<String, List<Signal>> entry : byCategory.entrySet()) {
            List<Signal> group = entry.getValue();
            measured.add(new CategoryDiagnostic(
                entry.getKey(),
                group.size(),
                (int) group.stream().filter(this::isResolved).count(),
                medianDays(group, history),
                resolvedShare(group),
                medianPriority(group),
                votesPerReport(group),
                List.of()
            ));
        }

        int withoutBaseline = (int) measured.stream()
            .filter(diagnostic -> diagnostic.reports() < thresholds.minCategoryReports())
            .count();

        List<CategoryDiagnostic> eligible = measured.stream()
            .filter(diagnostic -> diagnostic.reports() >= thresholds.minCategoryReports())
            .toList();

        // The standard is the best treatment this community already achieves, not its average.
        //
        // The average is the wrong yardstick and disguise the disparity it is supposed to reveal:
        // with half the categories at 2 days and half at 40, the community median lands at 21 and a
        // slow category reads as 1.9x rather than the 20x it is. A community cannot argue that
        // unequal treatment is unavoidable while one of its own categories is already doing better.
        CategoryDiagnostic reference = pickReference(eligible);

        List<CategoryDiagnostic> categories = new ArrayList<>();
        for (CategoryDiagnostic diagnostic : measured) {
            if (diagnostic.reports() < thresholds.minCategoryReports()) {
                // Counted rather than flagged: a flag here would be noise wearing the costume of a
                // finding, and it would teach people to ignore the flags.
                categories.add(withFlags(diagnostic, List.of("NO_BASELINE")));
                continue;
            }
            categories.add(withFlags(diagnostic, flagsFor(diagnostic, reference, thresholds)));
        }

        categories.sort(Comparator
            .comparingInt((CategoryDiagnostic diagnostic) -> diagnostic.flags().isEmpty() ? 1 : 0)
            .thenComparing(CategoryDiagnostic::category));

        List<String> disparate = categories.stream()
            .filter(diagnostic -> diagnostic.flags().stream().anyMatch(flag -> !flag.equals("NO_BASELINE")))
            .map(CategoryDiagnostic::category)
            .toList();

        return new BiasDiagnosticReport(
            VERSION,
            communityId.toString(),
            LocalDateTime.now(),
            thresholds,
            signals.size(),
            medianDays(signals, history),
            resolvedShare(signals),
            votesPerReport(signals),
            reference == null ? null : reference.category(),
            categories,
            disparate,
            withoutBaseline,
            scopeStatement(disparate.size(), withoutBaseline, reference)
        );
    }

    /**
     * Flags one category against the reference, with every comparison stated.
     *
     * <p>Exposed as a method rather than inlined so the whole rule is readable in one place, which
     * is the point of a diagnostic anybody is meant to trust.
     */
    private List<String> flagsFor(
        CategoryDiagnostic diagnostic,
        CategoryDiagnostic reference,
        Thresholds thresholds
    ) {
        List<String> flags = new ArrayList<>();
        if (reference == null || reference == diagnostic) {
            // Nothing to compare against, or this is the standard itself.
            return flags;
        }

        // Only compare resolution time once something in this community has resolved quickly. When
        // the reference is zero, "twice as slow" is zero and every category would flag.
        if (reference.medianDaysToResolve() > 0
            && diagnostic.medianDaysToResolve()
                >= Math.ceil(reference.medianDaysToResolve() * thresholds.slowerResolutionMultiplier())) {
            flags.add("SLOWER_RESOLUTION");
        }

        if (diagnostic.resolvedShare() + thresholds.lowerResolutionRateMargin()
            <= reference.resolvedShare()) {
            flags.add("LOWER_RESOLUTION_RATE");
        }

        if (reference.votesPerReport() > 0
            && diagnostic.votesPerReport()
                <= reference.votesPerReport() * thresholds.lowerAttentionMultiplier()) {
            flags.add("LOWER_ATTENTION");
        }

        return flags;
    }

    /**
     * The best-treated eligible category: fastest resolution, breaking ties on resolved share then
     * attention, and finally by name so the choice is deterministic across runs.
     */
    private CategoryDiagnostic pickReference(List<CategoryDiagnostic> eligible) {
        return eligible.stream()
            .min(Comparator
                .comparingLong(CategoryDiagnostic::medianDaysToResolve)
                .thenComparing(Comparator.comparingDouble(CategoryDiagnostic::resolvedShare).reversed())
                .thenComparing(Comparator.comparingDouble(CategoryDiagnostic::votesPerReport).reversed())
                .thenComparing(CategoryDiagnostic::category))
            .orElse(null);
    }

    private CategoryDiagnostic withFlags(CategoryDiagnostic diagnostic, List<String> flags) {
        return new CategoryDiagnostic(
            diagnostic.category(),
            diagnostic.reports(),
            diagnostic.resolved(),
            diagnostic.medianDaysToResolve(),
            diagnostic.resolvedShare(),
            diagnostic.medianPriorityScore(),
            diagnostic.votesPerReport(),
            flags
        );
    }

    /**
     * Says what this report is and is not, in the response, every time.
     *
     * <p>A "bias diagnostics" report that stayed quiet about lacking equity data would invite a
     * reader to conclude the platform has measured fairness across populations. It has not.
     */
    private String scopeStatement(int disparateCount, int withoutBaseline, CategoryDiagnostic reference) {
        StringBuilder statement = new StringBuilder();
        statement.append("This measures consistency of treatment between report categories, using data ")
            .append("the platform holds. It does NOT measure fairness across populations: the platform ")
            .append("has no equity, income, or demographic data, which is why no fairness weighting is ")
            .append("applied to any score. A weighting built on absent data would move real priorities ")
            .append("invisibly, so it is not built.");
        if (reference != null) {
            statement.append(" Comparisons are drawn against ")
                .append(reference.category())
                .append(", the best-treated category with enough reports here, on the principle that a ")
                .append("community cannot call unequal treatment unavoidable while one of its own ")
                .append("categories is already doing better.");
        } else {
            statement.append(" No category had enough reports, so no comparison was drawn at all.");
        }
        if (disparateCount > 0) {
            statement.append(" ").append(disparateCount)
                .append(" category(ies) are treated materially differently from that standard.");
        }
        if (withoutBaseline > 0) {
            statement.append(" ").append(withoutBaseline)
                .append(" category(ies) had too few reports to compare and are marked NO_BASELINE rather ")
                .append("than flagged.");
        }
        return statement.toString();
    }

    private long medianDays(List<Signal> signals, Map<UUID, List<SignalStatusEntry>> history) {
        List<Long> durations = new ArrayList<>();
        for (Signal signal : signals) {
            if (signal.getCreatedAt() == null) {
                continue;
            }
            history.getOrDefault(signal.getId(), List.of()).stream()
                .filter(entry -> entry.getCreatedAt() != null
                    && CLOSED_STATUSES.contains(entry.getStatusTo() == null
                        ? "" : entry.getStatusTo().toUpperCase(Locale.ROOT)))
                .findFirst()
                .ifPresent(entry -> durations.add(Math.max(
                    ChronoUnit.DAYS.between(
                        signal.getCreatedAt().toLocalDate(),
                        entry.getCreatedAt().toLocalDate()),
                    0)));
        }
        if (durations.isEmpty()) {
            return 0;
        }
        durations.sort(Long::compareTo);
        int mid = durations.size() / 2;
        return durations.size() % 2 == 1
            ? durations.get(mid)
            : Math.round((durations.get(mid - 1) + durations.get(mid)) / 2.0);
    }

    private double resolvedShare(List<Signal> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }
        long resolved = signals.stream().filter(this::isResolved).count();
        return round2((double) resolved / signals.size());
    }

    private double votesPerReport(List<Signal> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }
        return round2(signals.stream().mapToInt(Signal::getCommunityVotes).average().orElse(0));
    }

    private double medianPriority(List<Signal> signals) {
        if (signals.isEmpty()) {
            return 0.0;
        }
        List<Double> scores = signals.stream().map(Signal::getPriorityScore).sorted().toList();
        int mid = scores.size() / 2;
        return round2(scores.size() % 2 == 1
            ? scores.get(mid)
            : (scores.get(mid - 1) + scores.get(mid)) / 2.0);
    }

    private boolean isResolved(Signal signal) {
        return signal.getStatus() != null
            && CLOSED_STATUSES.contains(signal.getStatus().toUpperCase(Locale.ROOT));
    }

    private double round2(double value) {
        return java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .doubleValue();
    }
}