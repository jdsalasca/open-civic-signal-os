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
    private static final String NOT_ENOUGH_ACTIVITY =
        "Not enough reporting, decision, or execution activity was recorded in this period to infer trend strength yet.";

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
        MetricsWindow window = MetricsWindow.of(period);
        MetricsData data = loadData(communityId, window);

        long resolvedSignalsCount = data.signalsInPeriod().stream()
            .filter(signal -> RESOLVED_SIGNAL_STATUSES.contains(signal.getStatus()))
            .count();
        long completedTasksCount = data.tasksInPeriod().stream()
            .filter(task -> task.getStatus() == CommunityProjectStatus.DONE)
            .count();
        List<Long> resolutionHours = resolutionHours(data.resolutionEventsInPeriod(), data.signalsById());
        boolean lowData = data.signalsInPeriod().isEmpty()
            && data.decisionsInPeriod().isEmpty()
            && data.tasksInPeriod().isEmpty();
        LocalDateTime lastUpdatedAt = latestTimestamp(
            window.generatedAt(),
            data.allSignals().stream().map(Signal::getCreatedAt).toList(),
            data.allStatusEntries().stream().map(SignalStatusEntry::getCreatedAt).toList(),
            data.proposalsInPeriod().stream().map(CommunityProposal::getUpdatedAt).toList(),
            data.decisionsInPeriod().stream().map(CommunityDecision::getUpdatedAt).toList(),
            data.boards().stream().map(CommunityProjectBoard::getUpdatedAt).toList(),
            data.allTasks().stream().map(CommunityProjectTask::getUpdatedAt).toList()
        );

        return new CommunityTrustMetricsResponse(
            community.getId(),
            community.getName(),
            community.getSlug(),
            period.key(),
            window.startDate(),
            window.today(),
            window.generatedAt(),
            lastUpdatedAt,
            freshness(lastUpdatedAt, window.generatedAt()),
            lowData,
            lowData ? NOT_ENOUGH_ACTIVITY : null,
            buildCards(data, resolvedSignalsCount, completedTasksCount, resolutionHours),
            buildBreakdowns(data)
        );
    }

    private MetricsData loadData(UUID communityId, MetricsWindow window) {
        List<Signal> allSignals = signalRepository.findByCommunityId(communityId);
        List<Signal> signalsInPeriod = allSignals.stream()
            .filter(signal -> isWithin(signal.getCreatedAt(), window.startInclusive(), window.endExclusive()))
            .toList();
        List<SignalStatusEntry> allStatusEntries = allSignals.isEmpty()
            ? List.of()
            : signalStatusEntryRepository.findBySignalIdInOrderByCreatedAtAsc(
                allSignals.stream().map(Signal::getId).toList()
            );
        List<SignalStatusEntry> resolutionEventsInPeriod = allStatusEntries.stream()
            .filter(entry -> RESOLVED_SIGNAL_STATUSES.contains(entry.getStatusTo()))
            .filter(entry -> isWithin(entry.getCreatedAt(), window.startInclusive(), window.endExclusive()))
            .toList();
        Map<UUID, Signal> signalsById = allSignals.stream()
            .collect(Collectors.toMap(Signal::getId, signal -> signal));

        List<CommunityProposal> proposalsInPeriod = proposalRepository
            .findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .filter(proposal -> isWithin(proposal.getCreatedAt(), window.startInclusive(), window.endExclusive()))
            .toList();
        List<CommunityDecision> decisionsInPeriod = decisionRepository
            .findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(communityId).stream()
            .filter(decision -> isWithin(decision.getDecidedAt(), window.startInclusive(), window.endExclusive()))
            .toList();
        List<CommunityProjectBoard> boards = projectBoardRepository
            .findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId);
        List<CommunityProjectBoard> boardsInPeriod = boards.stream()
            .filter(board -> isWithin(board.getCreatedAt(), window.startInclusive(), window.endExclusive())
                || isWithin(board.getUpdatedAt(), window.startInclusive(), window.endExclusive()))
            .toList();
        List<CommunityProjectTask> allTasks = boards.isEmpty()
            ? List.of()
            : projectTaskRepository.findByProjectBoardIdIn(boards.stream().map(CommunityProjectBoard::getId).toList());
        Map<UUID, UUID> boardCommunityById = boards.stream()
            .collect(Collectors.toMap(CommunityProjectBoard::getId, CommunityProjectBoard::getCommunityId));
        List<CommunityProjectTask> tasksInPeriod = allTasks.stream()
            .filter(task -> boardCommunityById.containsKey(task.getProjectBoardId()))
            .filter(task -> isWithin(task.getCreatedAt(), window.startInclusive(), window.endExclusive())
                || isWithin(task.getUpdatedAt(), window.startInclusive(), window.endExclusive()))
            .toList();

        Set<UUID> activeContributors = activeContributors(
            signalsInPeriod, proposalsInPeriod, decisionsInPeriod, boardsInPeriod, tasksInPeriod
        );
        return new MetricsData(
            allSignals,
            signalsInPeriod,
            allStatusEntries,
            resolutionEventsInPeriod,
            signalsById,
            proposalsInPeriod,
            decisionsInPeriod,
            boards,
            boardsInPeriod,
            allTasks,
            tasksInPeriod,
            membershipRepository.countByCommunityId(communityId),
            activeContributors
        );
    }

    private Set<UUID> activeContributors(
        List<Signal> signals,
        List<CommunityProposal> proposals,
        List<CommunityDecision> decisions,
        List<CommunityProjectBoard> boards,
        List<CommunityProjectTask> tasks
    ) {
        Set<UUID> contributors = new java.util.LinkedHashSet<>();
        signals.stream().map(Signal::getAuthorId).filter(java.util.Objects::nonNull).forEach(contributors::add);
        proposals.stream().map(CommunityProposal::getAuthorId).filter(java.util.Objects::nonNull).forEach(contributors::add);
        decisions.stream().map(CommunityDecision::getDecidedBy).filter(java.util.Objects::nonNull).forEach(contributors::add);
        decisions.stream().map(CommunityDecision::getExecutionOwnerId).filter(java.util.Objects::nonNull).forEach(contributors::add);
        boards.stream().map(CommunityProjectBoard::getOwnerId).filter(java.util.Objects::nonNull).forEach(contributors::add);
        tasks.stream().map(CommunityProjectTask::getAssigneeId).filter(java.util.Objects::nonNull).forEach(contributors::add);
        return contributors;
    }

    private List<Long> resolutionHours(List<SignalStatusEntry> events, Map<UUID, Signal> signalsById) {
        return events.stream()
            .map(entry -> {
                Signal source = signalsById.get(entry.getSignalId());
                if (source == null || source.getCreatedAt() == null) {
                    return null;
                }
                return ChronoUnit.HOURS.between(source.getCreatedAt(), entry.getCreatedAt());
            })
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();
    }

    private List<TrustMetricCardResponse> buildCards(
        MetricsData data,
        long resolvedSignalsCount,
        long completedTasksCount,
        List<Long> resolutionHours
    ) {
        return List.of(
            resolutionCard(resolvedSignalsCount, data.signalsInPeriod().size()),
            participationCard(data.activeContributors().size(), data.membershipCount()),
            executionCard(completedTasksCount, data.tasksInPeriod().size()),
            resolutionTimeCard(resolutionHours)
        );
    }

    private TrustMetricCardResponse resolutionCard(long resolved, long total) {
        return new TrustMetricCardResponse(
            "resolution_rate",
            "Case closure rate",
            percentageValue(resolved, total),
            "percentage",
            "Share of issues reported during the selected period that are already closed or rejected.",
            "resolved_or_rejected_signals_created_in_period / total_signals_created_in_period",
            resolved + " of " + total + " reported issues have already reached a closed state."
        );
    }

    private TrustMetricCardResponse participationCard(long active, long members) {
        return new TrustMetricCardResponse(
            "participation_coverage",
            "Participation coverage",
            percentageValue(active, members),
            "percentage",
            "Share of community members who created reports, proposals, decisions, or execution work in the selected period.",
            "distinct_active_contributors_in_period / total_community_members",
            active + " of " + members + " known community members left a traceable contribution in this period."
        );
    }

    private TrustMetricCardResponse executionCard(long completed, long total) {
        return new TrustMetricCardResponse(
            "execution_completion_rate",
            "Execution completion",
            percentageValue(completed, total),
            "percentage",
            "Share of project tasks touched in this period that are already marked done.",
            "done_tasks_touched_in_period / total_tasks_touched_in_period",
            completed + " of " + total + " execution tasks active in this period are marked done."
        );
    }

    private TrustMetricCardResponse resolutionTimeCard(List<Long> resolutionHours) {
        return new TrustMetricCardResponse(
            "median_resolution_hours",
            "Median resolution time",
            resolutionHours.isEmpty() ? "0" : Long.toString(median(resolutionHours)),
            "hours",
            "Median hours between issue reporting and the first closed-state transition recorded during this period.",
            "median(hours_between_signal_created_at_and_first_resolved_or_rejected_event_in_period)",
            resolutionHours.isEmpty()
                ? "No resolved cases were recorded in this period yet."
                : "Computed from " + resolutionHours.size() + " resolved-case history entries."
        );
    }

    private List<TrustMetricBreakdownResponse> buildBreakdowns(MetricsData data) {
        return List.of(
            buildBreakdown(
                "signals_by_status",
                "Issue outcomes in this period",
                "How newly reported issues are distributed across current lifecycle states.",
                data.signalsInPeriod(),
                signal -> signal.getStatus().replace('_', ' ')
            ),
            buildBreakdown(
                "decisions_by_status",
                "Decisions recorded in this period",
                "How formal community decisions are distributed across their current execution states.",
                data.decisionsInPeriod(),
                decision -> decision.getDecisionStatus().name().replace('_', ' ')
            ),
            buildBreakdown(
                "tasks_by_stage",
                "Execution tasks touched in this period",
                "How active board tasks are distributed across TODO, IN PROGRESS, and DONE.",
                data.tasksInPeriod(),
                task -> task.getStatus().name().replace('_', ' ')
            ),
            buildBreakdown(
                "issue_categories",
                "Top issue categories",
                "Which issue categories generated the most reporting volume during the selected period.",
                data.signalsInPeriod(),
                Signal::getCategory
            )
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

    private record MetricsWindow(
        MetricsPeriod period,
        LocalDate startDate,
        LocalDate today,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive,
        LocalDateTime generatedAt
    ) {
        static MetricsWindow of(MetricsPeriod period) {
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(period.days() - 1L);
            return new MetricsWindow(
                period,
                startDate,
                today,
                startDate.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                LocalDateTime.now()
            );
        }
    }

    private record MetricsData(
        List<Signal> allSignals,
        List<Signal> signalsInPeriod,
        List<SignalStatusEntry> allStatusEntries,
        List<SignalStatusEntry> resolutionEventsInPeriod,
        Map<UUID, Signal> signalsById,
        List<CommunityProposal> proposalsInPeriod,
        List<CommunityDecision> decisionsInPeriod,
        List<CommunityProjectBoard> boards,
        List<CommunityProjectBoard> boardsInPeriod,
        List<CommunityProjectTask> allTasks,
        List<CommunityProjectTask> tasksInPeriod,
        long membershipCount,
        Set<UUID> activeContributors
    ) {}

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
