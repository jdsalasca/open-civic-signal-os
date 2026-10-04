package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.InstitutionalTicketHandoff;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.InstitutionalTicketHandoffRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The institutional action bridge: what we handed to a city, and whether it is being honoured.
 *
 * <p>The honest constraint first, because it shapes everything: <b>the platform cannot read a city
 * helpdesk.</b> It has no integration with the institution's queue, and it should not pretend to.
 * So every status here is one of two things:
 *
 * <ul>
 *   <li><b>Declared by a person.</b> Someone at the community records that the city acknowledged or
 *       resolved a ticket. That is a claim, and the record says who made it and when.
 *   <li><b>Derived from our own signal lifecycle.</b> If the community's own signal was resolved, the
 *       handoff is treated as resolved, because the resident's problem is what the handoff was for.
 * </ul>
 *
 * <p>What it never does is infer institutional state from anything else. A "synchronised" badge that
 * meant "we guessed" would be worse than no badge, because a community would act on it.
 *
 * <p>The SLA clock is the community's own, captured at handoff. A community tightening its target
 * next month must not retroactively turn a past on-time handoff into a late one.
 */
@Service
public class InstitutionalActionBridgeService {

    public static final String VERSION = "v1";

    private static final int DEFAULT_SLA_TARGET_DAYS = 30;
    private static final int MAX_SLA_TARGET_DAYS = 365;
    /** A larger batch should be split so a failure is attributable to a smaller set. */
    private static final int MAX_BATCH_SIZE = 200;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalStatusEntryRepository statusEntryRepository;
    private final InstitutionalTicketHandoffRepository handoffRepository;

    public InstitutionalActionBridgeService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalStatusEntryRepository statusEntryRepository,
        InstitutionalTicketHandoffRepository handoffRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.statusEntryRepository = statusEntryRepository;
        this.handoffRepository = handoffRepository;
    }

    public record HandoffRequest(
        UUID communityId,
        UUID signalId,
        String ticketRef,
        String externalCategoryCode,
        String externalTicketId,
        Integer slaTargetDays,
        String note
    ) {}

    /** A batch handoff: many signals, one category map, one SLA target. */
    public record BatchHandoffRequest(
        UUID communityId,
        List<UUID> signalIds,
        Map<String, String> categoryMap,
        Integer slaTargetDays,
        String note
    ) {}

    /** What happened to one signal in a batch. */
    public record BatchHandoffOutcome(
        UUID signalId,
        String signalTitle,
        String outcome,
        String detail
    ) {}

    public record BatchHandoffResult(
        String version,
        UUID communityId,
        int requested,
        int recorded,
        int skipped,
        List<BatchHandoffOutcome> outcomes,
        String interpretation
    ) {}

    /** One handoff with its derived SLA state and where that state came from. */
    public record HandoffView(
        UUID handoffId,
        UUID signalId,
        String signalTitle,
        String ticketRef,
        String externalCategoryCode,
        String externalTicketId,
        int slaTargetDays,
        long daysSinceHandoff,
        long daysOverTarget,
        String slaState,
        String statusSource,
        LocalDateTime handedOffAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        String note
    ) {}

    public record BridgeSummary(
        String version,
        UUID communityId,
        String communityName,
        int openHandoffs,
        int acknowledged,
        int resolved,
        int overdue,
        int dueSoon,
        List<HandoffView> handoffs,
        String interpretation,
        LocalDateTime generatedAt
    ) {}

    /**
     * Records that a report was handed to an institution.
     *
     * <p>Refuses a second open handoff for the same signal and ticket reference. Two clocks for one
     * complaint means nobody can say which one was late.
     */
    @Transactional
    public HandoffView recordHandoff(HandoffRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));
        Signal signal = signalRepository.findById(request.signalId())
            .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + request.signalId()));

        if (!community.getId().equals(signal.getCommunityId())) {
            throw new IllegalArgumentException(
                "The signal does not belong to this community, so handing it to an institution would "
                    + "attribute it to the wrong place.");
        }
        if (request.ticketRef() == null || request.ticketRef().isBlank()) {
            throw new IllegalArgumentException(
                "A ticket reference is required. Without it the handoff cannot be matched to anything "
                    + "the institution says back.");
        }
        if (request.externalCategoryCode() == null || request.externalCategoryCode().isBlank()) {
            throw new IllegalArgumentException(
                "An external category code is required. A handoff with no service code cannot be "
                    + "checked against what the city actually filed it as.");
        }

        int slaTarget = request.slaTargetDays() == null ? DEFAULT_SLA_TARGET_DAYS : request.slaTargetDays();
        if (slaTarget < 1 || slaTarget > MAX_SLA_TARGET_DAYS) {
            throw new IllegalArgumentException(
                "slaTargetDays must be between 1 and " + MAX_SLA_TARGET_DAYS + ", but was: " + slaTarget);
        }

        handoffRepository.findBySignalIdAndTicketRef(signal.getId(), request.ticketRef().trim())
            .ifPresent(existing -> {
                throw new ConflictException(
                    "This signal was already handed off under reference " + existing.getTicketRef()
                        + " on " + existing.getHandedOffAt() + ". A second handoff would create a second "
                        + "clock for the same complaint.");
            });

        InstitutionalTicketHandoff handoff = new InstitutionalTicketHandoff();
        handoff.setId(UUID.randomUUID());
        handoff.setCommunityId(community.getId());
        handoff.setSignalId(signal.getId());
        handoff.setTicketRef(request.ticketRef().trim());
        handoff.setExternalCategoryCode(request.externalCategoryCode().trim());
        handoff.setExternalTicketId(blankToNull(request.externalTicketId()));
        handoff.setHandedOffBy(user.getId());
        handoff.setHandedOffAt(LocalDateTime.now());
        handoff.setSlaTargetDays(slaTarget);
        handoff.setNote(blankToNull(request.note()));
        handoff = handoffRepository.save(handoff);

        return toView(handoff, signal);
    }

    /**
     * Records that the institution acknowledged or resolved a ticket.
     *
     * <p>Both are declarations by a person. The response says so, because a community reading
     * "resolved" needs to know whether the platform observed that or was told it.
     */
    @Transactional
    public HandoffView recordOutcome(
        UUID handoffId,
        boolean resolved,
        String note,
        String username
    ) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        InstitutionalTicketHandoff handoff = handoffRepository.findById(handoffId)
            .orElseThrow(() -> new ResourceNotFoundException("Handoff not found: " + handoffId));
        UUID signalId = handoff.getSignalId();
        Signal signal = signalRepository.findById(signalId)
            .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + signalId));

        LocalDateTime now = LocalDateTime.now();
        if (resolved) {
            if (handoff.getResolvedAt() == null) {
                handoff.setResolvedAt(now);
            }
            if (handoff.getAcknowledgedAt() == null) {
                // Resolving implies acknowledging. Leaving the acknowledgement empty would make the
                // record say the city never picked it up and then closed it.
                handoff.setAcknowledgedAt(now);
            }
        } else if (handoff.getAcknowledgedAt() == null) {
            handoff.setAcknowledgedAt(now);
        }
        handoff.setLastCheckedAt(now);
        if (note != null && !note.isBlank()) {
            handoff.setNote(note.trim());
        }
        handoff = handoffRepository.save(handoff);

        return toView(handoff, signal);
    }

    @Transactional(readOnly = true)
    public BridgeSummary summary(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        List<InstitutionalTicketHandoff> handoffs =
            handoffRepository.findByCommunityIdOrderByHandedOffAtDesc(communityId);

        List<HandoffView> views = new ArrayList<>();
        for (InstitutionalTicketHandoff handoff : handoffs) {
            Signal signal = signalRepository.findById(handoff.getSignalId()).orElse(null);
            if (signal != null) {
                views.add(toView(handoff, signal));
            }
        }

        int open = (int) views.stream().filter(view -> view.resolvedAt() == null).count();
        int acknowledged = (int) views.stream().filter(view -> view.acknowledgedAt() != null).count();
        int resolved = (int) views.stream().filter(view -> view.resolvedAt() != null).count();
        int overdue = (int) views.stream().filter(view -> "OVERDUE".equals(view.slaState())).count();
        int dueSoon = (int) views.stream().filter(view -> "DUE_SOON".equals(view.slaState())).count();

        return new BridgeSummary(
            VERSION,
            communityId,
            community.getName(),
            open,
            acknowledged,
            resolved,
            overdue,
            dueSoon,
            views,
            interpretation(views.size(), overdue),
            LocalDateTime.now()
        );
    }

    /**
     * Derives the SLA state from the community's own clock, and says where the status came from.
     *
     * <p>Resolution is taken from the handoff record first, then from the signal's own lifecycle. If
     * the community resolved the signal, the resident's problem is dealt with and the handoff is
     * treated as resolved, because that is what the handoff was for.
     */
    private HandoffView toView(InstitutionalTicketHandoff handoff, Signal signal) {
        LocalDateTime now = LocalDateTime.now();
        long daysSince = Math.max(
            ChronoUnit.DAYS.between(handoff.getHandedOffAt().toLocalDate(), now.toLocalDate()), 0);

        LocalDateTime resolvedAt = handoff.getResolvedAt();
        String statusSource;
        if (resolvedAt != null) {
            statusSource = "DECLARED_BY_COMMUNITY";
        } else if (signal.getStatus() != null
            && SignalStatus.isSettled(signal.getStatus())) {
            resolvedAt = latestClosure(signal.getId());
            statusSource = "DERIVED_FROM_SIGNAL_LIFECYCLE";
        } else {
            statusSource = handoff.getAcknowledgedAt() != null
                ? "DECLARED_BY_COMMUNITY"
                : "NO_INSTITUTIONAL_STATUS";
        }

        long daysOver = Math.max(daysSince - handoff.getSlaTargetDays(), 0);
        String slaState;
        if (resolvedAt != null) {
            // Late or not is judged against the moment it closed, not against today.
            long daysToClose = Math.max(
                ChronoUnit.DAYS.between(
                    handoff.getHandedOffAt().toLocalDate(), resolvedAt.toLocalDate()), 0);
            slaState = daysToClose > handoff.getSlaTargetDays() ? "CLOSED_LATE" : "CLOSED_ON_TIME";
        } else if (daysSince > handoff.getSlaTargetDays()) {
            slaState = "OVERDUE";
        } else if (daysSince >= handoff.getSlaTargetDays() * 0.8) {
            slaState = "DUE_SOON";
        } else {
            slaState = "WITHIN_TARGET";
        }

        return new HandoffView(
            handoff.getId(),
            signal.getId(),
            signal.getTitle(),
            handoff.getTicketRef(),
            handoff.getExternalCategoryCode(),
            handoff.getExternalTicketId(),
            handoff.getSlaTargetDays(),
            daysSince,
            daysOver,
            slaState,
            statusSource,
            handoff.getHandedOffAt(),
            handoff.getAcknowledgedAt(),
            resolvedAt,
            handoff.getNote()
        );
    }

    private LocalDateTime latestClosure(UUID signalId) {
        return statusEntryRepository.findBySignalIdOrderByCreatedAtDesc(signalId).stream()
            .filter(entry -> entry.getStatusTo() != null
                && SignalStatus.isSettled(entry.getStatusTo()))
            .map(SignalStatusEntry::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    /**
     * Says what this is and is not, on every summary.
     *
     * <p>A community reading "3 overdue" needs to know the platform did not observe that from the
     * city's system. It computed it from its own clock and from what people told it.
     */
    private String interpretation(int handoffs, int overdue) {
        StringBuilder text = new StringBuilder();
        text.append("This tracks what the platform handed to an institution and what it knows about the ")
            .append("outcome. It does NOT read the institution's system: the platform has no integration ")
            .append("with a city helpdesk. Every status is either declared by a community member or derived ")
            .append("from our own signal lifecycle, and each handoff says which. An overdue handoff means ")
            .append("the community's own target has passed, not that the city has failed, because the ")
            .append("platform cannot see the city's queue.");
        if (handoffs == 0) {
            text.append(" Nothing has been handed off yet.");
        } else if (overdue > 0) {
            text.append(" ").append(overdue)
                .append(" handoff(s) are past the community's target and worth a follow-up.");
        }
        return text.toString();
    }

    /**
     * Hands a whole batch over in one call.
     *
     * <p>The export from the municipal adapter is bulk, so recording handoffs one at a time meant a
     * community sending fifty tickets recorded fifty handoffs by hand. That is the gap this closes.
     *
     * <p>Partial success is the expected outcome and is reported per signal rather than as one
     * verdict. A batch where three of fifty were already handed off should record forty-seven and
     * say which three were skipped, not fail entirely and leave the community to work out why.
     *
     * <p>The ticket reference is derived from the signal id, the same way the export derives it, so
     * the reference a community quotes back to a city is the one the city received.
     */
    @Transactional
    public BatchHandoffResult recordBatchHandoff(BatchHandoffRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));

        if (request.signalIds() == null || request.signalIds().isEmpty()) {
            throw new IllegalArgumentException("At least one signal is required.");
        }
        if (request.signalIds().size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                "At most " + MAX_BATCH_SIZE + " signals can be handed off at once, but got: "
                    + request.signalIds().size() + ". A larger batch should be split so a failure is "
                    + "attributable to a smaller set.");
        }
        if (request.categoryMap() == null || request.categoryMap().isEmpty()) {
            throw new IllegalArgumentException(
                "A category map is required. Without a service code per category the handoffs cannot be "
                    + "checked against what the city actually filed them as.");
        }

        int slaTarget = request.slaTargetDays() == null ? DEFAULT_SLA_TARGET_DAYS : request.slaTargetDays();
        if (slaTarget < 1 || slaTarget > MAX_SLA_TARGET_DAYS) {
            throw new IllegalArgumentException(
                "slaTargetDays must be between 1 and " + MAX_SLA_TARGET_DAYS + ", but was: " + slaTarget);
        }

        Map<String, String> normalisedMap = new java.util.LinkedHashMap<>();
        request.categoryMap().forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                normalisedMap.put(key.trim().toLowerCase(Locale.ROOT), value == null ? "" : value.trim());
            }
        });

        List<BatchHandoffOutcome> outcomes = new ArrayList<>();
        int recorded = 0;
        int skipped = 0;

        for (UUID signalId : request.signalIds()) {
            Signal signal = signalRepository.findById(signalId).orElse(null);
            if (signal == null) {
                outcomes.add(new BatchHandoffOutcome(signalId, null, "SKIPPED", "Signal not found."));
                skipped++;
                continue;
            }
            if (!community.getId().equals(signal.getCommunityId())) {
                outcomes.add(new BatchHandoffOutcome(signalId, signal.getTitle(), "SKIPPED",
                    "The signal belongs to another community."));
                skipped++;
                continue;
            }

            String category = signal.getCategory() == null || signal.getCategory().isBlank()
                ? "unspecified"
                : signal.getCategory().trim().toLowerCase(Locale.ROOT);
            String code = normalisedMap.get(category);
            if (code == null || code.isBlank()) {
                // Excluded rather than defaulted, for the same reason the export excludes it: a
                // handoff filed against the wrong service code reaches the wrong department.
                outcomes.add(new BatchHandoffOutcome(signalId, signal.getTitle(), "SKIPPED",
                    "No service code mapped for category '" + category + "'."));
                skipped++;
                continue;
            }

            String ticketRef = InstitutionalTicketReference.forSignal(signal.getId());
            if (handoffRepository.findBySignalIdAndTicketRef(signal.getId(), ticketRef).isPresent()) {
                outcomes.add(new BatchHandoffOutcome(signalId, signal.getTitle(), "SKIPPED",
                    "Already handed off under " + ticketRef + "."));
                skipped++;
                continue;
            }

            InstitutionalTicketHandoff handoff = new InstitutionalTicketHandoff();
            handoff.setId(UUID.randomUUID());
            handoff.setCommunityId(community.getId());
            handoff.setSignalId(signal.getId());
            handoff.setTicketRef(ticketRef);
            handoff.setExternalCategoryCode(code);
            handoff.setHandedOffBy(user.getId());
            handoff.setHandedOffAt(LocalDateTime.now());
            handoff.setSlaTargetDays(slaTarget);
            handoff.setNote(blankToNull(request.note()));
            handoffRepository.save(handoff);

            outcomes.add(new BatchHandoffOutcome(signalId, signal.getTitle(), "RECORDED", ticketRef));
            recorded++;
        }

        return new BatchHandoffResult(
            VERSION,
            community.getId(),
            request.signalIds().size(),
            recorded,
            skipped,
            outcomes,
            batchInterpretation(recorded, skipped)
        );
    }

    private String batchInterpretation(int recorded, int skipped) {
        StringBuilder text = new StringBuilder();
        text.append(recorded).append(" handoff(s) recorded, ").append(skipped).append(" skipped. ");
        text.append("A skip is not a failure: it means the signal was already handed off, belongs to ")
            .append("another community, or has no service code mapped. Each outcome says which. ")
            .append("The platform still cannot read the institution's system, so these are records of ")
            .append("what was sent, not confirmations of receipt.");
        return text.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}