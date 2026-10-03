package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Surge detection: a formula with visible inputs, not a verdict in a badge.
 *
 * <p>This project's premise is that a ranking decision has to be explainable and checkable. A
 * "surge detected" marker with no visible inputs is exactly the black box the product exists to
 * avoid, so every result here travels with the windows it came from and the thresholds that
 * judged it.
 *
 * <p>Three choices worth defending:
 *
 * <ul>
 *   <li><b>The baseline is a median over several past windows, not a mean.</b> A mean over a
 *       window that already contains the spike is inflated by the spike, which makes a genuine
 *       surge look smaller the larger it gets. A median does not move for one outlier window,
 *       which is exactly the signal being looked for.
 *   <li><b>Baseline windows are adjacent and non-overlapping.</b> Overlapping windows let one
 *       busy afternoon appear in several windows and inflate the baseline with the very activity
 *       being measured.
 *   <li><b>An absolute minimum volume is required.</b> Without it a quiet zone going from one
 *       report to three is a "300% surge", and a dashboard full of those teaches people to
 *       ignore the marker.
 * </ul>
 *
 * <p>The unit is community plus category, not neighbourhood. The platform has no validated
 * neighbourhood geography: {@code locationLabel} is free text written by residents and does not
 * group reliably. Inventing a grouping from it would put confident-looking surge badges on
 * whichever spellings happened to match. Stating that limit is better than papering over it.
 */
@Service
public class SurgeDetectionService {

    public static final String VERSION = "v1";

    private static final int DEFAULT_BASELINE_WINDOWS = 4;
    private static final int DEFAULT_MIN_CURRENT = 5;
    private static final double DEFAULT_SURGE_MULTIPLIER = 2.0;
    private static final int DEFAULT_WINDOW_DAYS = 7;

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;

    public SurgeDetectionService(
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository,
        SignalRepository signalRepository
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
    }

    public record Thresholds(int baselineWindows, int minCurrentCount, double surgeMultiplier) {}

    public record WindowCounts(LocalDate start, LocalDate end, long count) {}

    /**
     * One zone's assessment with every input the verdict came from.
     *
     * <p>{@code baselineCounts} travels with the result because a human deciding whether to act
     * needs to see the individual windows, not only the median we summarised them into.
     */
    public record SurgeZone(
        String communityId,
        String communityName,
        String category,
        long currentCount,
        long baselineMedian,
        double ratio,
        String verdict,
        String reason,
        List<WindowCounts> baselineCounts
    ) {}

    public record SurgeReport(
        String version,
        Thresholds thresholds,
        LocalDate windowEnd,
        int windowDays,
        List<WindowCounts> baselinePeriods,
        List<SurgeZone> surges,
        List<SurgeZone> zonesEvaluated,
        int zonesConsidered,
        int zonesSuppressedByMinVolume,
        LocalDateTime generatedAt
    ) {}

    public Thresholds defaultThresholds() {
        return new Thresholds(DEFAULT_BASELINE_WINDOWS, DEFAULT_MIN_CURRENT, DEFAULT_SURGE_MULTIPLIER);
    }

    public int defaultWindowDays() {
        return DEFAULT_WINDOW_DAYS;
    }

    /**
     * Same visibility rule as the heat map: a caller sees the communities they belong to, or
     * everything when global analytics are allowed. Duplicated rather than extracted because
     * SignalGeoService keeps its own copy and adding a shared abstraction for one caller is not
     * worth the coupling. If a third caller appears, extract it then.
     */
    private List<Community> visibleCommunities(User user, boolean allowGlobal) {
        List<CommunityMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            Map<UUID, Community> byId = communityRepository.findAllById(
                    memberships.stream().map(CommunityMembership::getCommunityId).toList())
                .stream()
                .collect(Collectors.toMap(Community::getId, community -> community));
            return memberships.stream()
                .map(CommunityMembership::getCommunityId)
                .distinct()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        }
        return allowGlobal ? communityRepository.findAll() : List.of();
    }

    @Transactional(readOnly = true)
    public SurgeReport buildReport(
        String username,
        boolean allowGlobal,
        Integer windowDays,
        Integer baselineWindows,
        Integer minCurrentCount,
        Double surgeMultiplier,
        LocalDate windowEndExclusive
    ) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        List<Community> communities = visibleCommunities(user, allowGlobal);
        Thresholds thresholds = new Thresholds(
            baselineWindows == null ? DEFAULT_BASELINE_WINDOWS : baselineWindows,
            minCurrentCount == null ? DEFAULT_MIN_CURRENT : minCurrentCount,
            surgeMultiplier == null ? DEFAULT_SURGE_MULTIPLIER : surgeMultiplier
        );
        int days = windowDays == null ? DEFAULT_WINDOW_DAYS : windowDays;
        if (days < 1) {
            throw new IllegalArgumentException("windowDays must be at least 1.");
        }
        if (thresholds.baselineWindows() < 1) {
            throw new IllegalArgumentException("baselineWindows must be at least 1.");
        }
        if (thresholds.surgeMultiplier() <= 0) {
            throw new IllegalArgumentException("surgeMultiplier must be greater than 0.");
        }

        LocalDate endExclusive = windowEndExclusive == null ? LocalDate.now() : windowEndExclusive;
        List<WindowCounts> periods = baselinePeriods(endExclusive, days, thresholds.baselineWindows());

        Map<UUID, Community> communitiesById = new LinkedHashMap<>();
        communities.forEach(community -> communitiesById.put(community.getId(), community));

        Map<String, List<LocalDateTime>> byZone = new LinkedHashMap<>();
        if (!communitiesById.isEmpty()) {
            signalRepository.findByCommunityIdIn(communitiesById.keySet()).forEach(signal -> {
                if (signal.getCategory() == null || signal.getCategory().isBlank()) {
                    return;
                }
                String key = signal.getCommunityId() + "|" + signal.getCategory().trim().toLowerCase();
                byZone.computeIfAbsent(key, ignored -> new ArrayList<>()).add(signal.getCreatedAt());
            });
        }

        List<SurgeZone> evaluated = new ArrayList<>();
        int suppressed = 0;
        for (Map.Entry<String, List<LocalDateTime>> entry : byZone.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            UUID communityId = UUID.fromString(parts[0]);
            String category = parts[1];
            List<LocalDateTime> timestamps = entry.getValue();

            LocalDate currentStart = endExclusive.minusDays(days);
            long current = countInWindow(timestamps, currentStart, endExclusive);

            List<WindowCounts> windows = new ArrayList<>();
            for (WindowCounts period : periods) {
                windows.add(new WindowCounts(
                    period.start(), period.end(),
                    countInWindow(timestamps, period.start(), period.end())));
            }
            long baselineMedian = median(windows.stream().map(WindowCounts::count).toList());

            SurgeZone zone = assess(
                communityId.toString(), communitiesById.get(communityId).getName(), category,
                current, baselineMedian, windows, thresholds);

            if (zone.verdict().equals("SUPPRESSED_LOW_VOLUME")) {
                suppressed++;
            }
            evaluated.add(zone);
        }

        // SURGE first, then highest ratio, so the list a council officer reads starts with what
        // actually needs attention rather than alphabetically.
        evaluated.sort(Comparator
            .comparing(SurgeZone::verdict)
            .thenComparing(Comparator.comparingDouble(SurgeZone::ratio).reversed())
            .thenComparing(SurgeZone::category));

        List<SurgeZone> surges = evaluated.stream()
            .filter(zone -> zone.verdict().equals("SURGE"))
            .toList();

        return new SurgeReport(
            VERSION,
            thresholds,
            endExclusive,
            days,
            periods.stream()
                .map(period -> new WindowCounts(period.start(), period.end(), 0L))
                .toList(),
            surges,
            evaluated,
            evaluated.size(),
            suppressed,
            LocalDateTime.now()
        );
    }

    /**
     * The verdict, with the reason stated. Static so the formula is testable without a database,
     * and so a reviewer can read the whole decision in one place.
     */
    public static SurgeZone assess(
        String communityId,
        String communityName,
        String category,
        long current,
        long baselineMedian,
        List<WindowCounts> windows,
        Thresholds thresholds
    ) {
        double ratio = baselineMedian == 0
            ? (current > 0 ? Double.POSITIVE_INFINITY : 0.0)
            : (double) current / baselineMedian;

        if (current < thresholds.minCurrentCount()) {
            return new SurgeZone(communityId, communityName, category, current, baselineMedian, ratio,
                "SUPPRESSED_LOW_VOLUME",
                current + " report(s) this window is below the minimum of "
                    + thresholds.minCurrentCount() + ", so no percentage change is reported. A quiet "
                    + "zone tripling from one to three reports is not a surge.",
                windows);
        }

        if (ratio >= thresholds.surgeMultiplier()) {
            return new SurgeZone(communityId, communityName, category, current, baselineMedian, ratio,
                "SURGE",
                current + " reports against a baseline median of " + baselineMedian
                    + " is " + trim(ratio) + "x, at or above the " + thresholds.surgeMultiplier()
                    + "x threshold.",
                windows);
        }

        return new SurgeZone(communityId, communityName, category, current, baselineMedian, ratio,
            "STABLE",
            current + " reports against a baseline median of " + baselineMedian + " is "
                + trim(ratio) + "x, below the " + thresholds.surgeMultiplier() + "x threshold.",
            windows);
    }

/**
     * Adjacent, non-overlapping windows, oldest first, and <b>excluding the current window</b>.
     *
     * <p>Two failure modes this shape avoids. Overlapping windows would let one busy afternoon
     * appear in several windows and inflate the baseline with the activity being measured.
     * Including the current window would be worse still: the baseline would already contain the
     * spike, so a genuine surge would report a smaller ratio the larger it grew.
     *
     * <p>Window {@code i} therefore ends {@code i} days before the current window starts, for
     * {@code i} counting from 1.
     */
public static List<WindowCounts> baselinePeriods(LocalDate windowEndExclusive, int windowDays, int windows) {
        List<WindowCounts> periods = new ArrayList<>();
        for (int i = 1; i <= windows; i++) {
            LocalDate endExclusive = windowEndExclusive.minusDays((long) i * windowDays);
            periods.add(new WindowCounts(endExclusive.minusDays(windowDays), endExclusive, 0L));
        }
        java.util.Collections.reverse(periods);
        return periods;
    }

    private static long countInWindow(List<LocalDateTime> timestamps, LocalDate from, LocalDate toExclusive) {
        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toInclusive = toExclusive.atStartOfDay();
        return timestamps.stream()
            .filter(t -> t != null && !t.isBefore(fromInclusive) && t.isBefore(toInclusive))
            .count();
    }

    /**
     * A median survives the outlier window that a mean would absorb, which is the whole reason
     * it is used as the baseline.
     */
    public static long median(List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1
            ? sorted.get(mid)
            : Math.round((sorted.get(mid - 1) + sorted.get(mid)) / 2.0);
    }

    /** Infinity is correct for "no baseline", but it must not appear in a rendered report. */
    private static String trim(double ratio) {
        if (Double.isInfinite(ratio)) {
            return "no-baseline";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", ratio);
    }
}