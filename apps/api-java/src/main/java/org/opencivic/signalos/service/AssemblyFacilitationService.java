package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.AssemblyAgendaItem;
import org.opencivic.signalos.domain.CommunityAssembly;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.AssemblyAgendaItemRepository;
import org.opencivic.signalos.repository.CommunityAssemblyRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Townhall facilitation: the running order, and whether the meeting is keeping to it.
 *
 * <p>{@link CommunityAssemblyService} records what was deliberated and decided. This is the other
 * half: what the facilitator planned, and how the room is tracking against it. Without a plan, a
 * facilitator improvises, and the items at the end of the agenda are the ones that get dropped.
 *
 * <p>Three things this deliberately does not do:
 *
 * <ul>
 *   <li><b>It does not run the meeting.</b> It reports elapsed time against planned time. Whether to
 *       cut an item short is a judgement about the room, which the platform cannot make.
 *   <li><b>It does not enforce the plan.</b> Overrunning is reported, not prevented. A facilitator
 *       who decides an item deserves twice its slot is making a legitimate call.
 *   <li><b>It does not reorder by priority.</b> The order is the facilitator's. A platform that
 *       reordered a townhall agenda by score would be deciding what the community discusses.
 * </ul>
 */
@Service
public class AssemblyFacilitationService {

    public static final String VERSION = "v1";

    private static final int MAX_AGENDA_ITEMS = 50;
    private static final int MAX_ITEM_MINUTES = 240;

    private final UserRepository userRepository;
    private final CommunityAssemblyRepository assemblyRepository;
    private final AssemblyAgendaItemRepository agendaRepository;

    public AssemblyFacilitationService(
        UserRepository userRepository,
        CommunityAssemblyRepository assemblyRepository,
        AssemblyAgendaItemRepository agendaRepository
    ) {
        this.userRepository = userRepository;
        this.assemblyRepository = assemblyRepository;
        this.agendaRepository = agendaRepository;
    }

    public record AgendaItemRequest(
        String title,
        UUID subjectId,
        String subjectType,
        Integer plannedMinutes,
        String notes
    ) {}

    public record AgendaItemView(
        UUID itemId,
        int position,
        String title,
        UUID subjectId,
        String subjectType,
        int plannedMinutes,
        String notes
    ) {}

    /** One item with its place in the running order and the cumulative time to reach it. */
    public record AgendaItemProgress(
        UUID itemId,
        int position,
        String title,
        int plannedMinutes,
        int cumulativePlannedMinutes
    ) {}

    public record FacilitationView(
        String version,
        UUID assemblyId,
        UUID communityId,
        String title,
        String status,
        LocalDateTime openedAt,
        Long elapsedMinutes,
        int totalPlannedMinutes,
        int itemCount,
        List<AgendaItemView> agenda,
        List<AgendaItemProgress> runningOrder,
        String pacing,
        String interpretation
    ) {}

    /**
     * Replaces the whole agenda.
     *
     * <p>Replacing rather than appending, so a facilitator editing the plan mid-meeting does not end
     * up with two items at the same position. The unique index would refuse it anyway, and a
     * half-applied agenda is worse than a rejected one.
     */
    @Transactional
    public FacilitationView setAgenda(
        UUID assemblyId,
        UUID communityId,
        List<AgendaItemRequest> items,
        String username
    ) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        CommunityAssembly assembly = requireAssembly(assemblyId, communityId);

        if (assembly.getStatus() == CommunityAssembly.Status.CLOSED) {
            throw new ConflictException(
                "This assembly is closed. Changing the agenda afterwards would change what the record "
                    + "says was planned.");
        }
        List<AgendaItemRequest> requested = items == null ? List.of() : items;
        if (requested.size() > MAX_AGENDA_ITEMS) {
            throw new IllegalArgumentException(
                "At most " + MAX_AGENDA_ITEMS + " agenda items, but got: " + requested.size()
                    + ". A longer agenda is a schedule, not a meeting.");
        }

        agendaRepository.deleteByAssemblyId(assemblyId);
        int position = 1;
        for (AgendaItemRequest item : requested) {
            if (item.title() == null || item.title().isBlank()) {
                throw new IllegalArgumentException(
                    "Every agenda item needs a title. An untitled item cannot be referred to in the room.");
            }
            int planned = item.plannedMinutes() == null ? 10 : item.plannedMinutes();
            if (planned < 1 || planned > MAX_ITEM_MINUTES) {
                throw new IllegalArgumentException(
                    "plannedMinutes must be between 1 and " + MAX_ITEM_MINUTES + ", but was: " + planned);
            }

            AssemblyAgendaItem entity = new AssemblyAgendaItem();
            entity.setId(UUID.randomUUID());
            entity.setAssemblyId(assemblyId);
            entity.setPosition(position++);
            entity.setTitle(item.title().trim());
            entity.setSubjectId(item.subjectId());
            entity.setSubjectType(item.subjectType() == null || item.subjectType().isBlank()
                ? null : item.subjectType().trim());
            entity.setPlannedMinutes(planned);
            entity.setNotes(item.notes() == null || item.notes().isBlank() ? null : item.notes().trim());
            entity.setCreatedAt(LocalDateTime.now());
            agendaRepository.save(entity);
        }

        return view(assembly);
    }

    @Transactional(readOnly = true)
    public FacilitationView getFacilitation(UUID assemblyId, UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return view(requireAssembly(assemblyId, communityId));
    }

    private CommunityAssembly requireAssembly(UUID assemblyId, UUID communityId) {
        return assemblyRepository.findByIdAndCommunityId(assemblyId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Assembly not found: " + assemblyId));
    }

    private FacilitationView view(CommunityAssembly assembly) {
        List<AssemblyAgendaItem> items = agendaRepository.findByAssemblyIdOrderByPositionAsc(assembly.getId());

        List<AgendaItemView> agenda = new ArrayList<>();
        List<AgendaItemProgress> runningOrder = new ArrayList<>();
        int cumulative = 0;
        for (AssemblyAgendaItem item : items) {
            agenda.add(new AgendaItemView(
                item.getId(), item.getPosition(), item.getTitle(),
                item.getSubjectId(), item.getSubjectType(),
                item.getPlannedMinutes(), item.getNotes()));
            cumulative += item.getPlannedMinutes();
            runningOrder.add(new AgendaItemProgress(
                item.getId(), item.getPosition(), item.getTitle(),
                item.getPlannedMinutes(), cumulative));
        }

        // Measured against closedAt once the meeting has ended. Against now(), a closed assembly kept
        // counting for ever: "this meeting ran for 40 minutes" is a fact about the meeting, and a
        // record that reads 4000 minutes next month is not a record of anything. An assembly still
        // open legitimately reads live, which is the whole point of a facilitation view.
        Long elapsed = assembly.getOpenedAt() == null
            ? null
            : Math.max(
                ChronoUnit.MINUTES.between(
                    assembly.getOpenedAt(),
                    assembly.getClosedAt() == null ? LocalDateTime.now() : assembly.getClosedAt()),
                0);

        return new FacilitationView(
            VERSION,
            assembly.getId(),
            assembly.getCommunityId(),
            assembly.getTitle(),
            assembly.getStatus().name(),
            assembly.getOpenedAt(),
            elapsed,
            cumulative,
            agenda.size(),
            agenda,
            runningOrder,
            pacing(assembly, elapsed, cumulative),
            interpretation(assembly, elapsed, cumulative, agenda.size())
        );
    }

    /**
     * How the room is tracking, stated as a fact rather than a verdict.
     *
     * <p>"Overrunning" is not a failure. A facilitator who gives an item twice its slot is making a
     * legitimate call about the room, and the platform has no standing to call it wrong.
     */
    private String pacing(CommunityAssembly assembly, Long elapsed, int totalPlanned) {
        if (assembly.getStatus() != CommunityAssembly.Status.OPEN || elapsed == null) {
            return "NOT_STARTED";
        }
        if (totalPlanned == 0) {
            return "NO_PLAN";
        }
        if (elapsed > totalPlanned) {
            return "OVERRUNNING";
        }
        if (elapsed >= totalPlanned * 0.8) {
            return "NEARING_END";
        }
        return "ON_PLAN";
    }

    private String interpretation(CommunityAssembly assembly, Long elapsed, int totalPlanned, int itemCount) {
        StringBuilder text = new StringBuilder();
        text.append("This is the running order and how the room is tracking against it. The platform ")
            .append("does NOT run the meeting: whether to cut an item short is a judgement about the ")
            .append("room, which the platform cannot make. Overrunning is reported, not prevented, ")
            .append("because a facilitator who gives an item twice its slot may be making the right call. ")
            .append("The order is the facilitator's; the platform does not reorder an agenda by score, ")
            .append("because that would be deciding what the community discusses.");

        if (itemCount == 0) {
            text.append(" No agenda has been set, so there is nothing to pace against.");
            return text.toString();
        }
        if (assembly.getStatus() != CommunityAssembly.Status.OPEN) {
            text.append(" The assembly is not open, so no elapsed time is being tracked.");
            return text.toString();
        }
        text.append(" ").append(elapsed).append(" minute(s) elapsed against a planned ")
            .append(totalPlanned).append(".");
        if (elapsed > totalPlanned) {
            text.append(" The meeting is past its plan by ").append(elapsed - totalPlanned)
                .append(" minute(s).");
        }
        return text.toString();
    }
}