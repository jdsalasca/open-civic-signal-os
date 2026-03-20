package org.opencivic.signalos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityProjectStatus;
import org.opencivic.signalos.domain.CommunityProjectTask;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityProjectTaskRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.web.dto.CommunityTrustMetricsResponse;
import org.opencivic.signalos.web.dto.TrustMetricBreakdownItemResponse;
import org.opencivic.signalos.web.dto.TrustMetricBreakdownResponse;
import org.opencivic.signalos.web.dto.TrustMetricCardResponse;
import org.springframework.stereotype.Service;

@Service
public class CommunityTrustMetricsService {
    private static final Set<String> RESOLVED_SIGNAL_STATUSES = Set.of("RESOLVED", "REJECTED");

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository signalStatusEntryRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityDecisionRepository decisionRepository;
    private final CommunityProjectBoardRepository projectBoardRepository;
    private final CommunityProjectTaskRepository projectTaskRepository;

    public CommunityTrustMetricsService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository signalStatusEntryRepository,
        CommunityProposalRepository proposalRepository,
        CommunityDecisionRepository decisionRepository,
        CommunityProjectBoardRepository projectBoardRepository,
        CommunityProjectTaskRepository projectTaskRepository
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.signalRepository = signalRepository;
        this.signalStatusEntryRepository = signalStatusEntryRepository;
        this.proposalRepository = proposalRepository;
        this.decisionRepository = decisionRepository;
        this.projectBoardRepository = projectBoardRepository;
        this.projectTaskRepository = projectTaskRepository;
    }

    public CommunityTrustMetricsResponse getTrustMetrics(UUID communityId, String periodKey, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        return buildTrustMetrics(communityId, periodKey);
    }

    public CommunityTrustMetricsResponse getTrustMetricsForExport(UUID communityId, String periodKey) {
        return buildTrustMetrics(communityId, periodKey);
    }

    private CommunityTrustMetricsResponse buildTrustMetrics(UUID communityId, String periodKey) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found for trust metrics: " + communityId));
        MetricsPeriod period = MetricsPeriod.from(periodKey);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(period.days() - 1L);
        LocalDateTime startInclusive = startDate.atStartOfDay();
        LocalDateTime endExclusive = today.plusDays(1).atStartOfDay();
        LocalDateTime generatedAt = LocalDateTime.now();

        List<Signal> allSignals = signalRepository.findByCommunityId(communityId);
        List<Signal> signalsInPeriod = allSignals.stream()
            .filter(signal -> isWithin(signal.getCreatedAt(), startInclusive, endExclusive))
            .toList();
        List<SignalStatusEntry> allStatusEntries = allSignals.isEmpty()
            ? List.of()
            : signalStatusEntryRepository.findBySignalIdInOrderByCreatedAtAsc(
                allSignals.stream().map(Signal::getId).toList()
            );
        List<SignalStatusEntry> resolutionEventsInPeriod = allStatusEntries.stream()
            .filter(entry -> RESOLVED_SIGNAL_STATUSES.contains(entry.getStatusTo()))
            .filter(entry -> isWithin(entry.getCreatedAt(), startInclusive, endExclusive))
            .toList();
        Map<UUID, Signal> signalsById = allSignals.stream()
            .collect(Collectors.toMap(Signal::getId, signal -> signal));

        List<CommunityProposal> proposalsInPeriod = proposalRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .filter(proposal -> isWithin(proposal.getCreatedAt(), startInclusive, endExclusive))
            .toList();
        List<CommunityDecision> decisionsInPeriod = decisionRepository.findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(communityId).stream()
            .filter(decision -> isWithin(decision.getDecidedAt(), startInclusive, endExclusive))
            .toList();
        List<CommunityProjectBoard> boards = projectBoardRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId);
        List<CommunityProjectBoard> boardsInPeriod = boards.stream()
            .filter(board -> isWithin(board.getCreatedAt(), startInclusive, endExclusive) || isWithin(board.getUpdatedAt(), startInclusive, endExclusive))
            .toList();
        List<CommunityProjectTask> allTasks = boards.isEmpty()
            ? List.of()
            : projectTaskRepository.findByProjectBoardIdIn(boards.stream().map(CommunityProjectBoard::getId).toList());
        Map<UUID, UUID> boardCommunityById = boards.stream()
            .collect(Collectors.toMap(CommunityProjectBoard::getId, CommunityProjectBoard::getCommunityId));
        List<CommunityProjectTask> tasksInPeriod = allTasks.stream()
            .filter(task -> boardCommunityById.containsKey(task.getProjectBoardId()))
            .filter(task -> isWithin(task.getCreatedAt(), startInclusive, endExclusive) || isWithin(task.getUpdatedAt(), startInclusive, endExclusive))
            .toList();

        long membershipCount = membershipRepository.countByCommunityId(communityId);
        Set<UUID> activeContributors = new java.util.LinkedHashSet<>();
        signalsInPeriod.stream().map(Signal::getAuthorId).filter(java.util.Objects::nonNull).forEach(activeContributors::add);
        proposalsInPeriod.stream().map(CommunityProposal::getAuthorId).filter(java.util.Objects::nonNull).forEach(activeContributors::add);
        decisionsInPeriod.stream().map(CommunityDecision::getDecidedBy).filter(java.util.Objects::nonNull).forEach(activeContributors::add);
        decisionsInPeriod.stream().map(CommunityDecision::getExecutionOwnerId).filter(java.util.Objects::nonNull).forEach(activeContributors::add);
        boardsInPeriod.stream().map(CommunityProjectBoard::getOwnerId).filter(java.util.Objects::nonNull).forEach(activeContributors::add);
        tasksInPeriod.stream().map(CommunityProjectTask::getAssigneeId).filter(java.util.Objects::nonNull).forEach(activeContributors::add);

        long resolvedSignalsCount = signalsInPeriod.stream()
            .filter(signal -> RESOLVED_SIGNAL_STATUSES.contains(signal.getStatus()))
            .count();
        long completedTasksCount = tasksInPeriod.stream()
            .filter(task -> task.getStatus() == CommunityProjectStatus.DONE)
            .count();

        List<Long> resolutionHours = resolutionEventsInPeriod.stream()
            .map(entry -> {
                Signal sourceSignal = signalsById.get(entry.getSignalId());
                if (sourceSignal == null || sourceSignal.getCreatedAt() == null) {
                    return null;
                }
                return ChronoUnit.HOURS.between(sourceSignal.getCreatedAt(), entry.getCreatedAt());
            })
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();

        String resolutionRateValue = percentageValue(resolvedSignalsCount, signalsInPeriod.size());
        String participationValue = percentageValue(activeContributors.size(), membershipCount);
        String executionValue = percentageValue(completedTasksCount, tasksInPeriod.size());
        String medianResolutionValue = resolutionHours.isEmpty()
            ? "0"
            : Long.toString(median(resolutionHours));

        boolean lowData = signalsInPeriod.isEmpty() && decisionsInPeriod.isEmpty() && tasksInPeriod.isEmpty();
        String lowDataReason = lowData
            ? "Not enough reporting, decision, or execution activity was recorded in this period to infer trend strength yet."
            : null;

        LocalDateTime lastUpdatedAt = latestTimestamp(
            generatedAt,
            allSignals.stream().map(Signal::getCreatedAt).toList(),
            allStatusEntries.stream().map(SignalStatusEntry::getCreatedAt).toList(),
            proposalsInPeriod.stream().map(CommunityProposal::getUpdatedAt).toList(),
            decisionsInPeriod.stream().map(CommunityDecision::getUpdatedAt).toList(),
            boards.stream().map(CommunityProjectBoard::getUpdatedAt).toList(),
            allTasks.stream().map(CommunityProjectTask::getUpdatedAt).toList()
        );

        List<TrustMetricCardResponse> cards = List.of(
            new TrustMetricCardResponse(
                "resolution_rate",
                "Case closure rate",
                resolutionRateValue,
                "percentage",
                "Share of issues reported during the selected period that are already closed or rejected.",
                "resolved_or_rejected_signals_created_in_period / total_signals_created_in_period",
                resolvedSignalsCount + " of " + signalsInPeriod.size() + " reported issues have already reached a closed state."
            ),
            new TrustMetricCardResponse(
                "participation_coverage",
                "Participation coverage",
                participationValue,
                "percentage",
                "Share of community members who created reports, proposals, decisions, or execution work in the selected period.",
                "distinct_active_contributors_in_period / total_community_members",
                activeContributors.size() + " of " + membershipCount + " known community members left a traceable contribution in this period."
            ),
            new TrustMetricCardResponse(
                "execution_completion_rate",
                "Execution completion",
                executionValue,
                "percentage",
                "Share of project tasks touched in this period that are already marked done.",
                "done_tasks_touched_in_period / total_tasks_touched_in_period",
                completedTasksCount + " of " + tasksInPeriod.size() + " execution tasks active in this period are marked done."
            ),
            new TrustMetricCardResponse(
                "median_resolution_hours",
                "Median resolution time",
                medianResolutionValue,
                "hours",
                "Median hours between issue reporting and the first closed-state transition recorded during this period.",
                "median(hours_between_signal_created_at_and_first_resolved_or_rejected_event_in_period)",
                resolutionHours.isEmpty()
                    ? "No resolved cases were recorded in this period yet."
                    : "Computed from " + resolutionHours.size() + " resolved-case history entries."
            )
        );

        List<TrustMetricBreakdownResponse> breakdowns = List.of(
            buildBreakdown(
                "signals_by_status",
                "Issue outcomes in this period",
                "How newly reported issues are distributed across current lifecycle states.",
                signalsInPeriod,
                signal -> signal.getStatus().replace('_', ' ')
            ),
            buildBreakdown(
                "decisions_by_status",
                "Decisions recorded in this period",
                "How formal community decisions are distributed across their current execution states.",
                decisionsInPeriod,
                decision -> decision.getDecisionStatus().name().replace('_', ' ')
            ),
            buildBreakdown(
                "tasks_by_stage",
                "Execution tasks touched in this period",
                "How active board tasks are distributed across TODO, IN PROGRESS, and DONE.",
                tasksInPeriod,
                task -> task.getStatus().name().replace('_', ' ')
            ),
            buildBreakdown(
                "issue_categories",
                "Top issue categories",
                "Which issue categories generated the most reporting volume during the selected period.",
                signalsInPeriod,
                Signal::getCategory
            )
        );

        return new CommunityTrustMetricsResponse(
            community.getId(),
            community.getName(),
            community.getSlug(),
            period.key(),
            startDate,
            today,
            generatedAt,
            lastUpdatedAt,
            freshness(lastUpdatedAt, generatedAt),
            lowData,
            lowDataReason,
            cards,
            breakdowns
        );
    }

    private boolean isWithin(LocalDateTime timestamp, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return timestamp != null && !timestamp.isBefore(startInclusive) && timestamp.isBefore(endExclusive);
    }

    private LocalDateTime latestTimestamp(LocalDateTime fallback, Collection<LocalDateTime>... timestamps) {
        LocalDateTime latest = fallback;
        for (Collection<LocalDateTime> batch : timestamps) {
            for (LocalDateTime timestamp : batch) {
                if (timestamp != null && (latest == null || timestamp.isAfter(latest))) {
                    latest = timestamp;
                }
            }
        }
        return latest;
    }

    private String freshness(LocalDateTime timestamp, LocalDateTime generatedAt) {
        if (timestamp == null) {
            return "awaiting first update";
        }
        long minutes = Math.max(0L, ChronoUnit.MINUTES.between(timestamp, generatedAt));
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = Math.max(1L, ChronoUnit.HOURS.between(timestamp, generatedAt));
        if (hours < 48) {
            return hours + "h ago";
        }
        long days = Math.max(1L, ChronoUnit.DAYS.between(timestamp, generatedAt));
        return days + "d ago";
    }

    private long median(List<Long> sortedValues) {
        int size = sortedValues.size();
        if (size == 0) {
            return 0;
        }
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        }
        return Math.round((sortedValues.get((size / 2) - 1) + sortedValues.get(size / 2)) / 2.0d);
    }

    private String percentageValue(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0%";
        }
        BigDecimal percentage = BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
            .stripTrailingZeros();
        return percentage.toPlainString() + "%";
    }

    private <T> TrustMetricBreakdownResponse buildBreakdown(
        String key,
        String title,
        String description,
        List<T> source,
        java.util.function.Function<T, String> labelResolver
    ) {
        Map<String, Long> counts = source.stream()
            .collect(Collectors.groupingBy(
                item -> normalizeLabel(labelResolver.apply(item)),
                LinkedHashMap::new,
                Collectors.counting()
            ));
        List<TrustMetricBreakdownItemResponse> items = new ArrayList<>();
        long total = source.size();
        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
            .forEach(entry -> items.add(
                new TrustMetricBreakdownItemResponse(
                    entry.getKey(),
                    entry.getValue(),
                    total == 0 ? 0.0d : roundShare(entry.getValue(), total)
                )
            ));

        return new TrustMetricBreakdownResponse(key, title, description, items);
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unspecified";
        }
        return raw.trim();
    }

    private double roundShare(long value, long total) {
        if (total <= 0) {
            return 0.0d;
        }
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private record MetricsPeriod(String key, int days) {
        static MetricsPeriod from(String raw) {
            if (raw == null || raw.isBlank()) {
                return new MetricsPeriod("LAST_30_DAYS", 30);
            }
            return switch (raw) {
                case "LAST_7_DAYS" -> new MetricsPeriod("LAST_7_DAYS", 7);
                case "LAST_90_DAYS" -> new MetricsPeriod("LAST_90_DAYS", 90);
                default -> new MetricsPeriod("LAST_30_DAYS", 30);
            };
        }
    }
}
