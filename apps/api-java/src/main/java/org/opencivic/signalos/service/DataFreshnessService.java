package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data freshness monitoring: which civic surfaces have gone quiet, and whether we can tell why.
 *
 * <p>Per-record freshness strings already exist on dashboards. This is the monitoring view:
 * every surface a community depends on, its age, and a verdict with the arithmetic behind it.
 *
 * <p>The reason this exists rather than a second freshness badge: "no new reports" has two
 * causes that send a person to opposite places. Either the community has nothing to report, or
 * the channel that used to carry reports is broken. A monitor that cannot distinguish them
 * pages someone to chase a broken feed during a genuinely quiet month, and then stops trusting
 * the alert. So a verdict carries {@code cause}, and {@code UNKNOWN} is an explicit, honest
 * answer rather than a silent one.
 *
 * <p>Silence is judged against the community's own history, not a global threshold. A median of
 * the gaps between its own consecutive reports answers "how long does this place normally go
 * quiet". A community that reports weekly and has been silent for a month is worth a page; one
 * that files twice a year is not. Same median-over-outlier reasoning as the surge formula.
 */
@Service
public class DataFreshnessService {

    public static final String VERSION = "v1";

    /** Multiples of the community's own median gap before a surface is called stale. */
    private static final double STALE_GAP_MULTIPLE = 3.0;
    private static final double DORMANT_GAP_MULTIPLE = 10.0;

    /** Below this many historical events there is no cadence to compare against. */
    private static final int MIN_EVENTS_FOR_CADENCE = 4;

    public record SourceFreshness(
        String key,
        String label,
        LocalDate lastEventOn,
        Long daysSinceLast,
        Long medianGapDays,
        double multipleOfMedianGap,
        String verdict,
        String cause,
        String reason,
        int totalEvents
    ) {}

    public record FreshnessReport(
        String version,
        String communityId,
        String communityName,
        LocalDate generatedOn,
        boolean allHealthy,
        int staleCount,
        List<SourceFreshness> sources,
        String guidance
    ) {}

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository statusEntryRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityDecisionRepository decisionRepository;

    public DataFreshnessService(
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository statusEntryRepository,
        CommunityProposalRepository proposalRepository,
        CommunityDecisionRepository decisionRepository
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.statusEntryRepository = statusEntryRepository;
        this.proposalRepository = proposalRepository;
        this.decisionRepository = decisionRepository;
    }

    @Transactional(readOnly = true)
    public List<FreshnessReport> getReports(String username, boolean allowGlobal, UUID communityId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        List<Community> visible = visibleCommunities(user, allowGlobal);
        if (communityId != null) {
            visible = visible.stream()
                .filter(community -> community.getId().equals(communityId))
                .toList();
            if (visible.isEmpty()) {
                throw new ResourceNotFoundException("Community not visible: " + communityId);
            }
        }
        return visible.stream().map(community -> buildReport(community)).toList();
    }

    private FreshnessReport buildReport(Community community) {
        List<Signal> signals = signalRepository.findByCommunityId(community.getId());

        List<LocalDateTime> reportedAt = signals.stream()
            .map(Signal::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();
        List<LocalDateTime> proposedAt = proposalRepository
            .findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(community.getId()).stream()
            .map(CommunityProposal::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();
        List<LocalDateTime> decidedAt = decisionRepository
            .findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(community.getId()).stream()
            .map(CommunityDecision::getDecidedAt)
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();
        List<LocalDateTime> statusChangedAt = signals.isEmpty()
            ? List.of()
            : statusEntryRepository.findBySignalIdInOrderByCreatedAtAsc(
                    signals.stream().map(Signal::getId).toList()).stream()
                .map(SignalStatusEntry::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();

        List<SourceFreshness> sources = new ArrayList<>();
        sources.add(assess("REPORTS", "Community reports", reportedAt));
        sources.add(assess("PROPOSALS", "Proposals opened", proposedAt));
        sources.add(assess("DECISIONS", "Decisions recorded", decidedAt));
        sources.add(assess("STATUS_CHANGES", "Signal status changes", statusChangedAt));

        int stale = (int) sources.stream()
            .filter(source -> source.verdict().equals("STALE") || source.verdict().equals("DORMANT"))
            .count();

        return new FreshnessReport(
            VERSION,
            community.getId().toString(),
            community.getName(),
            LocalDate.now(),
            stale == 0,
            stale,
            sources,
            guidance(stale)
        );
    }

    /**
     * Verdict plus cause. The cause is the point: {@code UNKNOWN} means we can see silence and
     * cannot say why, which is the honest answer and tells a responder to check the intake path
     * rather than assume the community stopped caring.
     */
    public static SourceFreshness assess(String key, String label, List<LocalDateTime> events) {
        List<LocalDateTime> sorted = events.stream().sorted().toList();
        if (sorted.isEmpty()) {
            return new SourceFreshness(key, label, null, null, null, 0.0,
                "NO_HISTORY",
                "UNKNOWN",
                "No records exist yet, so there is no cadence to compare silence against. Treat as a "
                    + "setup question, not a dropout: a community that has never filed a proposal is "
                    + "not a community whose proposals stopped arriving.",
                0);
        }

        LocalDateTime last = sorted.get(sorted.size() - 1);
        long daysSince = Math.max(ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now()), 0);
        long medianGap = medianGapDays(sorted);

        if (sorted.size() < MIN_EVENTS_FOR_CADENCE || medianGap == 0) {
            return new SourceFreshness(key, label, last.toLocalDate(), daysSince, medianGap, 0.0,
                "NO_BASELINE",
                "UNKNOWN",
                sorted.size() + " record(s) is not enough history to establish a cadence ("
                    + MIN_EVENTS_FOR_CADENCE + " needed). Silence cannot be judged against this yet.",
                sorted.size());
        }

        double multiple = (double) daysSince / medianGap;

        if (multiple >= DORMANT_GAP_MULTIPLE) {
            return new SourceFreshness(key, label, last.toLocalDate(), daysSince, medianGap, multiple,
                "DORMANT",
                "UNKNOWN",
                "Silent for " + daysSince + " days against a typical gap of " + medianGap
                    + " days. Either intake has broken or the community genuinely has nothing to "
                    + "report; the data cannot distinguish those.",
                sorted.size());
        }
        if (multiple >= STALE_GAP_MULTIPLE) {
            return new SourceFreshness(key, label, last.toLocalDate(), daysSince, medianGap, multiple,
                "STALE",
                "UNKNOWN",
                "Silent for " + daysSince + " days against a typical gap of " + medianGap
                    + " days, which is over the " + STALE_GAP_MULTIPLE + "x threshold but under "
                    + DORMANT_GAP_MULTIPLE + "x.",
                sorted.size());
        }
        return new SourceFreshness(key, label, last.toLocalDate(), daysSince, medianGap, multiple,
            "FRESH",
            "ON_CADENCE",
            "Last recorded " + daysSince + " day(s) ago against a typical gap of " + medianGap
                + " days.",
            sorted.size());
    }

    /**
     * Median gap between consecutive events.
     *
     * <p>Median rather than mean for the same reason the surge baseline uses one: one long gap,
     * caused by a holiday or a freeze, would otherwise become the normal gap for that community
     * and mask every future alert.
     */
    static long medianGapDays(List<LocalDateTime> sorted) {
        if (sorted.size() < 2) {
            return 0;
        }
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            gaps.add(ChronoUnit.DAYS.between(sorted.get(i - 1).toLocalDate(), sorted.get(i).toLocalDate()));
        }
        List<Long> sortedGaps = new ArrayList<>(gaps);
        sortedGaps.sort(Long::compareTo);
        int mid = sortedGaps.size() / 2;
        return sortedGaps.size() % 2 == 1
            ? sortedGaps.get(mid)
            : Math.round((sortedGaps.get(mid - 1) + sortedGaps.get(mid)) / 2.0);
    }

    private String guidance(int staleCount) {
        if (staleCount == 0) {
            return "Every monitored surface is within its usual cadence for this community.";
        }
        return staleCount + " surface(s) are outside their usual cadence. Cause is UNKNOWN by design: "
            + "confirm the intake path is still delivering before assuming the community stopped "
            + "reporting, and check whether a municipal integration was the channel carrying this data.";
    }

    private List<Community> visibleCommunities(User user, boolean allowGlobal) {
        List<CommunityMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            java.util.Map<UUID, Community> byId = communityRepository.findAllById(
                    memberships.stream().map(CommunityMembership::getCommunityId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Community::getId, community -> community));
            return memberships.stream()
                .map(CommunityMembership::getCommunityId)
                .distinct()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        }
        return allowGlobal ? communityRepository.findAll() : List.of();
    }
}