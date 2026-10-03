package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.SignalAgingBucketResponse;
import org.opencivic.signalos.web.dto.SignalAgingItemResponse;
import org.opencivic.signalos.web.dto.SignalAgingResponse;
import org.opencivic.signalos.web.dto.SignalAgingTrendPointResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backend-owned issue aging and SLA risk.
 *
 * ponytail: SLA targets are a single flat number for every case. Per-category or
 * per-priority targets need a policy record; add one when communities ask for it
 * rather than guessing the weights now.
 */
@Service
public class SignalAgingService {
    static final long DEFAULT_SLA_TARGET_DAYS = 30;
    static final int TREND_DAYS = 30;
    static final int MAX_AT_RISK_ITEMS = 20;

    private static final List<String> RESOLVED_STATUSES = List.of("RESOLVED", "CLOSED");
    private static final List<String> UNRESOLVED_EXCLUSIONS = List.of("RESOLVED", "CLOSED", "REJECTED", "FLAGGED");

    private static final String RISK_ON_TRACK = "ON_TRACK";
    private static final String RISK_AT_RISK = "AT_RISK";
    private static final String RISK_BREACHED = "BREACHED";

    /** Fraction of the target at which a case is called "at risk" rather than on track. */
    static final double AT_RISK_RATIO = 0.8;

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository statusEntryRepository;

    public SignalAgingService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository statusEntryRepository
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.statusEntryRepository = statusEntryRepository;
    }

    @Transactional(readOnly = true)
    public SignalAgingResponse getAging(UUID communityId, String username, Long slaTargetDays) {
        User user = communityAccessService.getCurrentUser(username);
        if (communityId != null) {
            communityAccessService.requireMembership(user.getId(), communityId);
            communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        }
        long slaTarget = slaTargetDays == null || slaTargetDays < 1 ? DEFAULT_SLA_TARGET_DAYS : slaTargetDays;
        LocalDateTime now = LocalDateTime.now();

        List<Signal> unresolved = communityId == null
            ? signalRepository.findByStatusNotInOrderByCreatedAtAsc(UNRESOLVED_EXCLUSIONS)
            : signalRepository.findByStatusNotInAndCommunityIdOrderByCreatedAtAsc(UNRESOLVED_EXCLUSIONS, communityId);

        List<SignalAgingItemResponse> items = unresolved.stream()
            .map(signal -> toItem(signal, now, slaTarget))
            .toList();

        long atRisk = items.stream().filter(item -> RISK_AT_RISK.equals(item.slaRisk())).count();
        long breached = items.stream().filter(item -> RISK_BREACHED.equals(item.slaRisk())).count();

        return new SignalAgingResponse(
            communityId,
            now.toString(),
            slaTarget,
            items.size(),
            atRisk,
            breached,
            medianAgeDays(items),
            buildAgeBuckets(items),
            items.stream()
                .filter(item -> !RISK_ON_TRACK.equals(item.slaRisk()))
                .sorted(Comparator.comparingLong(SignalAgingItemResponse::daysOverTarget).reversed())
                .limit(MAX_AT_RISK_ITEMS)
                .toList(),
            buildTrend(communityId, now)
        );
    }

    public static String resolveRisk(long ageDays, long slaTargetDays) {
        if (ageDays > slaTargetDays) {
            return RISK_BREACHED;
        }
        if (ageDays >= Math.floor(slaTargetDays * AT_RISK_RATIO)) {
            return RISK_AT_RISK;
        }
        return RISK_ON_TRACK;
    }

    private SignalAgingItemResponse toItem(Signal signal, LocalDateTime now, long slaTargetDays) {
        LocalDateTime createdAt = signal.getCreatedAt() == null ? now : signal.getCreatedAt();
        long ageDays = Math.max(0, java.time.Duration.between(createdAt, now).toDays());
        return new SignalAgingItemResponse(
            signal.getId(),
            signal.getTitle(),
            signal.getCategory(),
            signal.getStatus(),
            signal.getPriorityScore(),
            ageDays,
            slaTargetDays,
            resolveRisk(ageDays, slaTargetDays),
            Math.max(0, ageDays - slaTargetDays),
            createdAt
        );
    }

    private static long medianAgeDays(List<SignalAgingItemResponse> items) {
        if (items.isEmpty()) {
            return 0;
        }
        List<Long> ages = items.stream().map(SignalAgingItemResponse::ageDays).sorted().toList();
        int mid = ages.size() / 2;
        return ages.size() % 2 == 1 ? ages.get(mid) : (ages.get(mid - 1) + ages.get(mid)) / 2;
    }

    private static List<SignalAgingBucketResponse> buildAgeBuckets(List<SignalAgingItemResponse> items) {
        long fresh = 0;
        long aging = 0;
        long stale = 0;
        long overdue = 0;
        for (SignalAgingItemResponse item : items) {
            if (item.slaRisk().equals(RISK_BREACHED)) {
                overdue++;
            } else if (item.slaRisk().equals(RISK_AT_RISK)) {
                stale++;
            } else if (item.ageDays() >= 7) {
                aging++;
            } else {
                fresh++;
            }
        }
        return List.of(
            new SignalAgingBucketResponse("FRESH_0_7", fresh),
            new SignalAgingBucketResponse("AGING_7_14", aging),
            new SignalAgingBucketResponse("STALE_14_PLUS", stale),
            new SignalAgingBucketResponse("OVERDUE", overdue)
        );
    }

    private List<SignalAgingTrendPointResponse> buildTrend(UUID communityId, LocalDateTime now) {
        LocalDate startDay = now.toLocalDate().minusDays(TREND_DAYS - 1L);
        LocalDateTime since = startDay.atStartOfDay();

        List<Signal> created = communityId == null
            ? signalRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(since)
            : signalRepository.findByCommunityIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(communityId, since);

        Map<LocalDate, Long> createdByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> resolvedByDay = new LinkedHashMap<>();
        for (LocalDate day = startDay; !day.isAfter(now.toLocalDate()); day = day.plusDays(1)) {
            createdByDay.put(day, 0L);
            resolvedByDay.put(day, 0L);
        }
        created.forEach(signal -> bump(createdByDay, signal.getCreatedAt()));
        statusEntryRepository
            .findByStatusToInAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(RESOLVED_STATUSES, since)
            .stream()
            .filter(entry -> communityId == null || belongsToCommunity(entry, communityId))
            .forEach(entry -> bump(resolvedByDay, entry.getCreatedAt()));

        List<SignalAgingTrendPointResponse> points = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> day : createdByDay.entrySet()) {
            points.add(new SignalAgingTrendPointResponse(
                day.getKey().toString(),
                day.getValue(),
                resolvedByDay.getOrDefault(day.getKey(), 0L)
            ));
        }
        return points;
    }

    private boolean belongsToCommunity(SignalStatusEntry entry, UUID communityId) {
        return signalRepository.findById(entry.getSignalId())
            .map(signal -> communityId.equals(signal.getCommunityId()))
            .orElse(false);
    }

    private static void bump(Map<LocalDate, Long> counts, LocalDateTime timestamp) {
        if (timestamp == null) {
            return;
        }
        counts.computeIfPresent(timestamp.toLocalDate(), (day, count) -> count + 1);
    }
}