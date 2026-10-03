package org.opencivic.signalos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDigestPublication;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityDigestPublicationRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weekly civic digest: top unresolved problems and what got resolved, for one closed ISO week.
 *
 * <p>Generation is separated from delivery on purpose. Generating a digest is deterministic,
 * testable and harmless; sending one reaches residents. Wiring this into the email connector is the
 * next slice, and it needs a decision about who on the community side is accountable when a digest
 * goes out wrong.
 *
 * <p>Three properties, all of which come from mistakes this project has already made once:
 *
 * <ul>
 *   <li><b>A closed ISO week, not "the last seven days".</b> A digest pinned to a range whose end
 *       moves produces different content every time it is regenerated, so a resident who saved last
 *       week's bulletin could never check it. The week is Monday to Sunday, addressed as
 *       {@code 2026-W13}.
 *   <li><b>No wall-clock reads in the content.</b> Regenerating a past week must reproduce it
 *       exactly. The only live value is {@code generatedAt}, which describes the run.
 *   <li><b>Every item carries its reason.</b> AGENTS.md requires every list to expose why an item is
 *       ranked where it is. A digest that lists ten titles and no reasoning is a popularity board.
 * </ul>
 */
@Service
public class WeeklyDigestService {

    public static final String VERSION = "v1";

    private static final int DEFAULT_ITEM_LIMIT = 10;
    private static final int MAX_ITEM_LIMIT = 50;
    private static final Set<String> CLOSED_STATUSES = Set.of("RESOLVED", "CLOSED", "REJECTED");
    private static final DateTimeFormatter WEEK_FORMAT =
        DateTimeFormatter.ofPattern("YYYY-'W'ww", Locale.ROOT);

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository statusEntryRepository;
    private final CommunityDigestPublicationRepository publicationRepository;

    public WeeklyDigestService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository statusEntryRepository,
        CommunityDigestPublicationRepository publicationRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.statusEntryRepository = statusEntryRepository;
        this.publicationRepository = publicationRepository;
    }

    /** One item in the digest, with the score and the reason it ranks where it does. */
    public record DigestItem(
        UUID signalId,
        String title,
        String category,
        String status,
        String locationLabel,
        double priorityScore,
        int daysOpen,
        String whyRanked
    ) {}

    public record DigestWeek(
        String key,
        LocalDate startDate,
        LocalDate endDate,
        String previousKey
    ) {}

    public record WeeklyDigest(
        String version,
        UUID communityId,
        String communityName,
        DigestWeek week,
        List<DigestItem> topUnresolved,
        int resolvedThisWeek,
        int rejectedThisWeek,
        int reportedThisWeek,
        int stillOpenTotal,
        String body,
        String contentHash,
        boolean published,
        LocalDateTime publishedAt,
        LocalDateTime generatedAt
    ) {}

    /**
     * Resolve a week key, or default to the previous completed week.
     *
     * <p>Defaulting to the previous week rather than the current one for the same reason the
     * transparency report does: on Wednesday the current week is three days of data, and mailing
     * that as "this week's digest" would misdescribe the community.
     */
    public DigestWeek resolveWeek(String weekKey) {
        LocalDate monday;
        String trimmed = weekKey == null ? "" : weekKey.trim();
        if (trimmed.isEmpty()) {
            // Previous completed week, moved back to its Monday.
            monday = LocalDate.now(ZoneOffset.UTC).minusWeeks(1).with(DayOfWeek.MONDAY);
        } else {
            try {
                // ISO_WEEK_DATE parses YYYY-Www-D, so day 1 is Monday. A pattern without a day
                // field cannot parse a week on its own, which is why the explicit -1 is required.
                monday = LocalDate.parse(trimmed + "-1", DateTimeFormatter.ISO_WEEK_DATE);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                    "week must be formatted YYYY-Www, for example 2026-W13, but was: " + trimmed);
            }
        }
        LocalDate previousMonday = monday.minusWeeks(1);
        return new DigestWeek(
            WEEK_FORMAT.format(monday),
            monday,
            monday.plusDays(7),
            WEEK_FORMAT.format(previousMonday)
        );
    }

    @Transactional(readOnly = true)
    public WeeklyDigest buildDigest(UUID communityId, String weekKey, Integer limit, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return compose(communityId, weekKey, limit);
    }

    @Transactional
    public WeeklyDigest publishDigest(UUID communityId, String weekKey, Integer limit, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        WeeklyDigest digest = compose(communityId, weekKey, limit);

        publicationRepository.findByCommunityIdAndWeekKey(communityId, digest.week().key())
            .ifPresent(existing -> {
                throw new ConflictException(
                    "A digest for " + digest.week().key() + " was already published on "
                        + existing.getPublishedAt() + ". Re-sending a weekly bulletin to residents is "
                        + "not something to do by accident; regenerate and review it instead, or "
                        + "publish a later week."
                );
            });

        CommunityDigestPublication publication = new CommunityDigestPublication();
        publication.setId(UUID.randomUUID());
        publication.setCommunityId(communityId);
        publication.setWeekKey(digest.week().key());
        publication.setContentHash(digest.contentHash());
        publication.setItemCount(digest.topUnresolved().size());
        publication.setUnresolvableItemCount(digest.stillOpenTotal());
        publication.setPublishedBy(user.getId());
        publication.setPublishedAt(LocalDateTime.now());
        publication.setBody(digest.body());
        publicationRepository.save(publication);

        return new WeeklyDigest(
            digest.version(), digest.communityId(), digest.communityName(), digest.week(),
            digest.topUnresolved(), digest.resolvedThisWeek(), digest.rejectedThisWeek(),
            digest.reportedThisWeek(), digest.stillOpenTotal(), digest.body(), digest.contentHash(),
            true, publication.getPublishedAt(), digest.generatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CommunityDigestPublication> history(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return publicationRepository.findByCommunityIdOrderByWeekKeyDesc(communityId);
    }

    private WeeklyDigest compose(UUID communityId, String weekKey, Integer limit) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        int effectiveLimit = limit == null ? DEFAULT_ITEM_LIMIT : limit;
        if (effectiveLimit < 1 || effectiveLimit > MAX_ITEM_LIMIT) {
            throw new IllegalArgumentException(
                "limit must be between 1 and " + MAX_ITEM_LIMIT + ", but was: " + limit);
        }

        DigestWeek week = resolveWeek(weekKey);
        List<Signal> signals = signalRepository.findByCommunityId(communityId);
        java.util.Map<UUID, List<SignalStatusEntry>> history = signals.isEmpty()
            ? java.util.Map.of()
            : statusEntryRepository
                .findBySignalIdInOrderByCreatedAtAsc(signals.stream().map(Signal::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(SignalStatusEntry::getSignalId));

        List<Signal> reportedThisWeek = signals.stream()
            .filter(signal -> inWeek(signal.getCreatedAt(), week))
            .toList();

        int resolvedThisWeek = 0;
        int rejectedThisWeek = 0;
        for (Signal signal : signals) {
            for (SignalStatusEntry entry : history.getOrDefault(signal.getId(), List.of())) {
                if (!inWeek(entry.getCreatedAt(), week) || entry.getStatusTo() == null) {
                    continue;
                }
                String to = entry.getStatusTo().toUpperCase(Locale.ROOT);
                if ("REJECTED".equals(to)) {
                    rejectedThisWeek++;
                } else if (CLOSED_STATUSES.contains(to)) {
                    resolvedThisWeek++;
                }
            }
        }

        // Unresolved as of the END of the reported week, from the audit trail rather than the mutable
        // status column. A digest for a past week has to describe the backlog as it stood then; using
        // the live status would make a March digest absorb everything that happened since, which is
        // the same non-determinism the closed week exists to prevent.
        List<Signal> unresolved = signals.stream()
            .filter(signal -> isUnresolvedAtWeekEnd(signal, history, week))
            .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed()
                .thenComparing(Signal::getId))
            .toList();

        List<DigestItem> top = unresolved.stream()
            .limit(effectiveLimit)
            .map(signal -> toItem(signal, week))
            .toList();

        String body = renderBody(community, week, top, reportedThisWeek.size(),
            resolvedThisWeek, rejectedThisWeek, unresolved.size(), effectiveLimit);
        String hash = sha256(body);

        boolean published = publicationRepository
            .findByCommunityIdAndWeekKey(communityId, week.key()).isPresent();

        return new WeeklyDigest(
            VERSION, communityId, community.getName(), week, top,
            resolvedThisWeek, rejectedThisWeek, reportedThisWeek.size(), unresolved.size(),
            body, hash, published, null, LocalDateTime.now()
        );
    }

    /**
     * The "why" for one item, stated as the components the ranking actually uses.
     *
     * <p>Not a sentence the model made up: the same four inputs the score is built from, so a reader
     * can check the claim against the published formula.
     */
    private DigestItem toItem(Signal signal, DigestWeek week) {
        int daysOpen = signal.getCreatedAt() == null
            ? 0
            : (int) Math.max(ChronoUnit.DAYS.between(
                signal.getCreatedAt().toLocalDate(), week.startDate()), 0);

        List<String> reasons = new ArrayList<>();
        reasons.add("urgency " + signal.getUrgency() + "/5");
        reasons.add("impact " + signal.getImpact() + "/5");
        reasons.add(signal.getAffectedPeople() + " people affected");
        reasons.add(signal.getCommunityVotes() + " community votes");

        return new DigestItem(
            signal.getId(),
            signal.getTitle(),
            signal.getCategory(),
            signal.getStatus(),
            signal.getLocationLabel(),
            signal.getPriorityScore(),
            daysOpen,
            String.join(", ", reasons)
        );
    }

    private String renderBody(
        Community community,
        DigestWeek week,
        List<DigestItem> top,
        int reported,
        int resolved,
        int rejected,
        int stillOpen,
        int limit
    ) {
        StringBuilder body = new StringBuilder();
        body.append("# ").append(community.getName())
            .append(": weekly digest ").append(week.key()).append(System.lineSeparator());
        body.append(System.lineSeparator());
        body.append("Week of ").append(week.startDate())
            .append(" to ").append(week.endDate().minusDays(1)).append(".").append(System.lineSeparator());
        body.append(System.lineSeparator());

        body.append(reported).append(" report(s) came in, ")
            .append(resolved).append(" were resolved and ")
            .append(rejected).append(" were rejected with a reason.")
            .append(System.lineSeparator());
        body.append(stillOpen).append(" remain open in total.")
            .append(System.lineSeparator());
        body.append(System.lineSeparator());

        body.append("## Still unresolved, highest priority first").append(System.lineSeparator());
        body.append(System.lineSeparator());
        if (top.isEmpty()) {
            body.append("Nothing is outstanding.").append(System.lineSeparator());
        } else {
            int position = 1;
            for (DigestItem item : top) {
                body.append(position).append(". ").append(item.title())
                    .append(" (").append(item.category()).append(")")
                    .append(System.lineSeparator());
                // AGENTS.md: every list exposes why an item is ranked where it is.
                body.append("   score ").append(item.priorityScore())
                    .append(", open ").append(item.daysOpen()).append(" day(s)")
                    .append(", ").append(item.whyRanked())
                    .append(System.lineSeparator());
                position++;
            }
            if (top.size() == limit) {
                body.append(System.lineSeparator())
                    .append("Only the top ").append(limit)
                    .append(" are listed here; the full backlog is on the platform.")
                    .append(System.lineSeparator());
            }
        }
        return body.toString();
    }

    /**
     * Was this signal part of the open backlog at the end of the reported week?
     *
     * <p>Three cases, and each one matters:
     *
     * <ul>
     *   <li>Created after the week ended: not part of that week's backlog at all.
     *   <li>Closed before the week ended: resolved by then, so not outstanding.
     *   <li>Created before the week ended and not yet closed: outstanding, including an old report
     *       that is still open. A backlog digest that omitted long-standing items would hide exactly
     *       the problems the digest exists to surface.
     * </ul>
     *
     * <p>Closure is read from the audit trail rather than the live status column, because the column
     * carries no timestamp and cannot be asked what it said last March.
     */
    private boolean isUnresolvedAtWeekEnd(
        Signal signal,
        java.util.Map<UUID, List<SignalStatusEntry>> history,
        DigestWeek week
    ) {
        if (signal.getCreatedAt() == null || !signal.getCreatedAt().toLocalDate().isBefore(week.endDate())) {
            return false;
        }
        LocalDateTime weekEnd = week.endDate().atStartOfDay();
        return history.getOrDefault(signal.getId(), List.of()).stream()
            .noneMatch(entry -> entry.getCreatedAt() != null
                && entry.getCreatedAt().isBefore(weekEnd)
                && entry.getStatusTo() != null
                && CLOSED_STATUSES.contains(entry.getStatusTo().toUpperCase(Locale.ROOT)));
    }

    private boolean inWeek(LocalDateTime timestamp, DigestWeek week) {
        if (timestamp == null) {
            return false;
        }
        LocalDate date = timestamp.toLocalDate();
        return !date.isBefore(week.startDate()) && date.isBefore(week.endDate());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable, cannot seal a digest", ex);
        }
    }
}