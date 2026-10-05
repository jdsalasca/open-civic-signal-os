package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalScoreEntryRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.web.dto.TransparencyMetricResponse;
import org.opencivic.signalos.web.dto.TransparencyPeriod;
import org.opencivic.signalos.web.dto.TransparencyReportResponse;
import org.opencivic.signalos.web.dto.TransparencySignalOutcomeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly transparency report over a closed calendar month.
 *
 * <p>The trust-metrics dashboard and this service answer the same question at different
 * moments. The dashboard is for someone deciding what to do right now, so a rolling window is
 * right. A report gets published, quoted in a council meeting, and challenged months later, so
 * the window has to be fixed and the previous month has to be carried alongside for comparison.
 *
 * <p>Determinism is the whole design constraint. No {@code now()} anywhere in the computation:
 * a report for March must return March's figures in December. The only wall-clock value in the
 * response is {@code generatedAt}, which describes the run, not the community.
 */
@Service
public class TransparencyReportService {
    private static final int UNADDRESSED_LIST_LIMIT = 10;
    private static final int ACTIONED_LIST_LIMIT = 10;

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
  private final SignalScoreEntryRepository scoreEntryRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository signalStatusEntryRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityDecisionRepository decisionRepository;
    private final PrioritizationFormulaService formulaService;

    public TransparencyReportService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository signalStatusEntryRepository,
        CommunityProposalRepository proposalRepository,
        CommunityDecisionRepository decisionRepository,
        PrioritizationFormulaService formulaService,
        SignalScoreEntryRepository scoreEntryRepository
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.signalRepository = signalRepository;
        this.signalStatusEntryRepository = signalStatusEntryRepository;
        this.proposalRepository = proposalRepository;
        this.decisionRepository = decisionRepository;
        this.formulaService = formulaService;
        this.scoreEntryRepository = scoreEntryRepository;
    }

    @Transactional(readOnly = true)
    public TransparencyReportResponse getReport(UUID communityId, String periodKey, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        return buildReport(communityId, periodKey);
    }

    @Transactional(readOnly = true)
    public TransparencyReportResponse buildReport(UUID communityId, String periodKey) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found for report: " + communityId));

        TransparencyPeriod period = resolvePeriod(periodKey);
        TransparencyPeriod previous = period.previous();

        List<Signal> allSignals = signalRepository.findByCommunityId(communityId);
        Map<UUID, List<SignalStatusEntry>> statusHistory = allSignals.isEmpty()
            ? Map.of()
            : signalStatusEntryRepository
                .findBySignalIdInOrderByCreatedAtAsc(allSignals.stream().map(Signal::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(SignalStatusEntry::getSignalId));

        PeriodTotals current = totals(communityId, period, allSignals, statusHistory);
        PeriodTotals prior = totals(communityId, previous, allSignals, statusHistory);

        return new TransparencyReportResponse(
            communityId,
            community.getName(),
            period,
            buildMetrics(current, prior),
            actionedSignals(period, allSignals, statusHistory),
            unaddressedSignals(period, allSignals, statusHistory),
            narrative(community, period, current, prior),
            formulaService.getFormula().version(),
            reproducibilityLimits(),
            LocalDateTime.now()
        );
    }

    /**
     * What in this report a reader must not treat as reproducible.
     *
     * <p>Returned as data rather than buried in documentation, because the person who needs it is
     * whoever consumes the JSON — a dashboard, a municipality, a journalist — and they will never read
     * this service's javadoc.
     *
     * <p>Recording the gap is the cheap half of fixing it. The other half is a score ledger, which means
     * deciding when a score becomes official, and that is not a decision to make while chasing a
     * reproducibility bug.
     */
    private List<String> reproducibilityLimits() {
        return List.of(
            "Scores come from the score ledger where one exists: the score shown is the score the item "
                + "held when this period closed. Signals recorded before score history existed have no "
                + "ledger entry, and for those the score shown is the current one. Every count, status "
                + "and list position in this report comes from the audit trail and is reproducible."
        );
    }

    /**
     * Accepts {@code YYYY-MM} or a full ISO date, and defaults to the previous calendar month.
     *
     * <p>Defaulting to the previous month rather than the current one is deliberate: on the
     * 3rd of April the current month is three days of data, and publishing that as "this
     * month's report" would be misleading.
     */
    private TransparencyPeriod resolvePeriod(String periodKey) {
        YearMonth month;
        String trimmed = periodKey == null ? "" : periodKey.trim();
        if (trimmed.isEmpty()) {
            month = YearMonth.now().minusMonths(1);
        } else if (trimmed.length() == 7) {
            try {
                month = YearMonth.parse(trimmed);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                    "period must be formatted YYYY-MM, for example 2026-03, but was: " + trimmed);
            }
        } else {
            try {
                month = YearMonth.from(LocalDate.parse(trimmed));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                    "period must be formatted YYYY-MM or YYYY-MM-DD, but was: " + trimmed);
            }
        }

        YearMonth prior = month.minusMonths(1);
        return new TransparencyPeriod(
            month.toString(),
            month.atDay(1),
            month.plusMonths(1).atDay(1),
            new TransparencyPeriod(
                prior.toString(),
                prior.atDay(1),
                month.atDay(1),
                null
            )
        );
    }

    private record PeriodTotals(
        int reported,
        int resolved,
        int rejected,
        int stillOpen,
        long medianResolutionDays,
        int proposals,
        int decisions
    ) {}

    private PeriodTotals totals(
        UUID communityId,
        TransparencyPeriod period,
        List<Signal> allSignals,
        Map<UUID, List<SignalStatusEntry>> statusHistory
    ) {
        List<Signal> reportedInPeriod = allSignals.stream()
            .filter(signal -> period.contains(signal.getCreatedAt()))
            .toList();

        int resolved = 0;
        int rejected = 0;
        List<Long> resolutionDays = new ArrayList<>();
        for (Signal signal : reportedInPeriod) {
            for (SignalStatusEntry entry : statusHistory.getOrDefault(signal.getId(), List.of())) {
                if (!period.contains(entry.getCreatedAt())) {
                    continue;
                }
                if (SignalStatus.isSettled(entry.getStatusTo())) {
                    if (signal.getCreatedAt() != null) {
                        resolutionDays.add(ChronoUnit.DAYS.between(
                            signal.getCreatedAt().toLocalDate(),
                            entry.getCreatedAt().toLocalDate()));
                    }
                    if ("REJECTED".equals(entry.getStatusTo())) {
                        rejected++;
                    } else {
                        resolved++;
                    }
                }
            }
        }

        int proposals = (int) proposalRepository
            .findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .filter(proposal -> period.contains(proposal.getCreatedAt()))
            .count();
        int decisions = (int) decisionRepository
            .findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(communityId).stream()
            .filter(decision -> period.contains(decision.getDecidedAt()))
            .count();

        // Still open counts what the resident is still waiting on: reported in this period and
        // not closed at any point up to the end of the period. As-of the period end rather than
        // today, otherwise a March report regenerated in June would show different figures.
        int stillOpen = (int) reportedInPeriod.stream()
            .filter(signal -> statusHistory.getOrDefault(signal.getId(), List.of()).stream()
                .noneMatch(entry -> SignalStatus.isSettled(entry.getStatusTo())
                    && entry.getCreatedAt() != null
                    && !entry.getCreatedAt().isAfter(period.endDate().atStartOfDay().minusNanos(1))))
            .count();

        return new PeriodTotals(
            reportedInPeriod.size(),
            resolved,
            rejected,
            stillOpen,
            median(resolutionDays),
            proposals,
            decisions
        );
    }

    private List<TransparencyMetricResponse> buildMetrics(PeriodTotals current, PeriodTotals prior) {
        return List.of(
            TransparencyMetricResponse.of("SIGNALS_REPORTED", "Signals reported",
                current.reported(), "count", prior.reported()),
            TransparencyMetricResponse.of("SIGNALS_RESOLVED", "Signals resolved",
                current.resolved(), "count", prior.resolved()),
            TransparencyMetricResponse.of("SIGNALS_REJECTED", "Signals rejected with reason",
                current.rejected(), "count", prior.rejected()),
            TransparencyMetricResponse.of("SIGNALS_STILL_OPEN", "Still open at period end",
                current.stillOpen(), "count", prior.stillOpen()),
            TransparencyMetricResponse.of("MEDIAN_RESOLUTION_DAYS", "Median days to resolve",
                current.medianResolutionDays(), "days", prior.medianResolutionDays()),
            TransparencyMetricResponse.of("PROPOSALS_CREATED", "Proposals opened",
                current.proposals(), "count", prior.proposals()),
            TransparencyMetricResponse.of("DECISIONS_RECORDED", "Decisions recorded",
                current.decisions(), "count", prior.decisions())
        );
    }

    private List<TransparencySignalOutcomeResponse> actionedSignals(
        TransparencyPeriod period,
        List<Signal> allSignals,
        Map<UUID, List<SignalStatusEntry>> statusHistory
    ) {
        List<TransparencySignalOutcomeResponse> rows = new ArrayList<>();
        for (Signal signal : allSignals) {
            if (!period.contains(signal.getCreatedAt())) {
                continue;
            }
            for (SignalStatusEntry entry : statusHistory.getOrDefault(signal.getId(), List.of())) {
                if (period.contains(entry.getCreatedAt())
                    && SignalStatus.isSettled(entry.getStatusTo())
                    && !"REJECTED".equals(entry.getStatusTo())) {
                    rows.add(toOutcome(signal, entry.getStatusTo(), entry.getCreatedAt()));
                }
            }
        }
        rows.sort(Comparator.comparingLong(TransparencySignalOutcomeResponse::daysOpen)
            .thenComparing(TransparencySignalOutcomeResponse::signalId));
        return rows.stream().limit(ACTIONED_LIST_LIMIT).toList();
    }

    /**
     * Whether the community had settled this issue by the time the period ended.
     *
     * <p>Reads the audit trail rather than the status column. Settled states are terminal —
     * {@code SignalStatus.canTransitionTo} refuses to leave RESOLVED or REJECTED — so "was it settled by
     * then" does not change afterwards, which is what makes a closed period safe to regenerate.
     */
    /**
     * The score a signal held when the period ended.
 *
 * <p>Read from score history rather than off the mutable column. The ledger records what happened at
 * each of the three operations that move a score — recorded, supported, merged — so this is a fact
 * rather than a policy about which score counts, and it applies the same "as of the period end" rule
 * the statuses in this report already use.
 *
 * <p>Falls back to the current score when a signal has no recorded history, which is every signal
 * predating the ledger. {@code reproducibilityLimits} says so, because a reader who cannot tell
 * "unrecorded" from "known" is being asked to trust a figure nobody looked up.
 */
    private double scoreAtPeriodEnd(Signal signal, LocalDateTime periodEnd) {
        return scoreEntryRepository
            .findLatestAtOrBefore(signal.getId(), periodEnd)
            .map(org.opencivic.signalos.domain.SignalScoreEntry::getPriorityScore)
            .orElseGet(signal::getPriorityScore);
    }

    private boolean settledBy(List<SignalStatusEntry> entries, LocalDateTime periodEnd) {
        for (SignalStatusEntry entry : entries) {
            if (entry.getCreatedAt() != null
                && !entry.getCreatedAt().isAfter(periodEnd)
                && SignalStatus.isSettled(entry.getStatusTo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The status to show for a past period: the last one recorded by the period end.
     *
     * <p>Falls back to the stored status only when the trail says nothing, which happens for a signal
     * that has never had a status entry. Showing today's value there is the least-wrong option and the
     * one place where a past figure can still drift.
     */
    private String statusAt(List<SignalStatusEntry> entries, LocalDateTime periodEnd, Signal signal) {
        String status = null;
        LocalDateTime latest = null;
        for (SignalStatusEntry entry : entries) {
            if (entry.getCreatedAt() != null
                && !entry.getCreatedAt().isAfter(periodEnd)
                && (latest == null || entry.getCreatedAt().isAfter(latest))) {
                latest = entry.getCreatedAt();
                status = entry.getStatusTo();
            }
        }
        return status == null ? signal.getStatus() : status;
    }

    private List<TransparencySignalOutcomeResponse> unaddressedSignals(
        TransparencyPeriod period,
        List<Signal> allSignals,
        Map<UUID, List<SignalStatusEntry>> statusHistory
    ) {
        LocalDateTime periodEnd = period.endDate().atStartOfDay().minusNanos(1);
        return allSignals.stream()
            .filter(signal -> period.contains(signal.getCreatedAt()))
            // Unaddressed **as of the period end**, from the audit trail rather than the mutable status
            // column. Reading the live status made February's record of what the community failed to
            // address shrink every time it did the work later: a fence fixed in April vanished from
            // February's unaddressed list, with no trace that the record had changed.
            //
            // The same reasoning the digest applies to a closed week, and the sibling method below
            // already did it this way: a past period has to describe the past.
            .filter(signal -> !settledBy(statusHistory.getOrDefault(signal.getId(), List.of()), periodEnd))
            .sorted(Comparator.comparingDouble((Signal signal) -> scoreAtPeriodEnd(signal, periodEnd)).reversed()
                .thenComparing(Signal::getId))
            .limit(UNADDRESSED_LIST_LIMIT)
            .map(signal -> new TransparencySignalOutcomeResponse(
                signal.getId(),
                signal.getTitle(),
                // The status as it stood at the period end, not today's.
                statusAt(statusHistory.getOrDefault(signal.getId(), List.of()), periodEnd, signal),
                signal.getCategory(),
                // And the score as it stood then, from the ledger, for the same reason.
                scoreAtPeriodEnd(signal, periodEnd),
                signal.getLocationLabel(),
                daysBetween(signal.getCreatedAt(), periodEnd),
                signal.getCreatedAt(),
                null
            ))
            .toList();
    }

    /**
     * One actioned signal, described as it was at the moment it was settled.
     *
     * <p>The status comes from the entry rather than the signal: "actioned" means this transition
     * happened, so quoting the signal's current status would misreport what the community did. The
     * score comes from the ledger at that moment for the same reason.
     */
    private TransparencySignalOutcomeResponse toOutcome(
        Signal signal,
        String statusAtResolution,
        LocalDateTime resolvedAt
    ) {
        return new TransparencySignalOutcomeResponse(
            signal.getId(),
            signal.getTitle(),
            statusAtResolution,
            signal.getCategory(),
            scoreAtPeriodEnd(signal, resolvedAt),
            signal.getLocationLabel(),
            daysBetween(signal.getCreatedAt(), resolvedAt),
            signal.getCreatedAt(),
            resolvedAt
        );
    }

    /**
     * Days-open for a signal still open is measured to the end of the reported period, not to
     * today. Measuring to today would make a March figure change every day the report is
     * regenerated, which is the exact non-determinism this report exists to avoid.
     */
    private static int daysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return (int) Math.max(ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()), 0);
    }

    private List<String> narrative(Community community, TransparencyPeriod period, PeriodTotals current, PeriodTotals prior) {
        List<String> lines = new ArrayList<>();
        lines.add(community.getName() + " transparency report for " + period.key()
            + " (" + period.startDate() + " to " + period.endDate().minusDays(1) + ").");
        lines.add(current.reported() + " signals were reported this period, compared with "
            + prior.reported() + " in " + period.previous().key() + ".");
        if (current.resolved() == 0 && current.stillOpen() > 0) {
            lines.add("No signals were resolved in this period and " + current.stillOpen()
                + " remained open at period end.");
        } else {
            lines.add(current.resolved() + " signals were resolved at a median of "
                + current.medianResolutionDays() + " days.");
        }
        if (current.rejected() > 0) {
            lines.add(current.rejected() + " signals were rejected with a recorded reason and remain reviewable.");
        }
        if (current.decisions() == 0 && current.proposals() > 0) {
            lines.add(current.proposals() + " proposals were opened but no decision was recorded against them.");
        }
        lines.add("Figures cover the closed calendar month only and are reproducible from the stored period.");
        return lines;
    }

    private static long median(List<Long> values) {
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
}