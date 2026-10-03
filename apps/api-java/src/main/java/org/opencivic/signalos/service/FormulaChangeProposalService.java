package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.FormulaChangeProposal;
import org.opencivic.signalos.domain.PrioritizationFormula;
import org.opencivic.signalos.domain.ProposedWeights;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.FormulaChangeProposalRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proposing a change to the prioritization formula, and showing what it would do first.
 *
 * <p>The valuable half of this is the preview. A community arguing about weights is arguing about a
 * number; what they actually need to see is that a proposal moves forty percent of the backlog, and
 * which items rise and fall. That is computable from the real signals, and it is the evidence
 * AGENTS.md requires with any scoring change.
 *
 * <p>Two things this deliberately does <b>not</b> do:
 *
 * <ul>
 *   <li><b>It does not apply anything.</b> Approving a proposal records a decision. Applying it means
 *       changing {@link PrioritizationFormula}, bumping the formula version, and writing an ADR.
 *       A row update that could change what a resident sees would be the black box this project
 *       forbids, so no scoring code reads these records.
 *   <li><b>It does not predict the future.</b> The preview reorders the backlog that exists now.
 *       Changing the formula changes what people report and vote on, so the backlog under the new
 *       formula would not be this one. The interpretation says so rather than letting a decider read
 *       the numbers as a forecast.
 * </ul>
 */
@Service
public class FormulaChangeProposalService {

    public static final String VERSION = "v1";

    private static final int MAX_MOVERS = 10;
    private static final int MAX_PREVIEW_SIGNALS = 2000;
    private static final Set<String> CLOSED_STATUSES = Set.of("RESOLVED", "CLOSED", "REJECTED");

    private final SignalRepository signalRepository;
    private final UserRepository userRepository;
    private final FormulaChangeProposalRepository proposalRepository;
    private final ObjectMapper objectMapper;

    public FormulaChangeProposalService(
        SignalRepository signalRepository,
        UserRepository userRepository,
        FormulaChangeProposalRepository proposalRepository,
        ObjectMapper objectMapper
    ) {
        this.signalRepository = signalRepository;
        this.userRepository = userRepository;
        this.proposalRepository = proposalRepository;
        this.objectMapper = objectMapper;
    }

    public record Mover(
        UUID signalId,
        String title,
        int currentPosition,
        int proposedPosition,
        int positionDelta
    ) {}

    public record ImpactPreview(
        String version,
        String currentExpression,
        String proposedExpression,
        int sampleSize,
        int positionsMoved,
        int positionsUnchanged,
        double movedShare,
        List<Mover> biggestMovers,
        String interpretation,
        LocalDateTime computedAt
    ) {}

    public record ProposalView(
        UUID proposalId,
        String title,
        String rationale,
        ProposedWeights currentWeights,
        ProposedWeights proposedWeights,
        ImpactPreview preview,
        String status,
        UUID proposedBy,
        LocalDateTime proposedAt,
        UUID decidedBy,
        LocalDateTime decidedAt,
        String decisionNote,
        String applicationNote
    ) {}

    /**
     * What would change. A pure computation over the current backlog; nothing is stored and nothing
     * is applied, so it is safe to call with any weights a community wants to consider.
     */
    @Transactional(readOnly = true)
    public ImpactPreview preview(ProposedWeights proposed, Integer limit) {
        List<Signal> backlog = currentBacklog(limit);
        return computePreview(proposed, backlog);
    }

    @Transactional
    public ProposalView propose(String title, String rationale, ProposedWeights proposed, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A proposal title is required.");
        }
        if (rationale == null || rationale.isBlank()) {
            // A scoring change with no stated reason is unreviewable later, which is the whole point
            // of recording it.
            throw new IllegalArgumentException(
                "A rationale is required. A scoring change nobody explained cannot be judged after the fact.");
        }

        List<Signal> backlog = currentBacklog(null);
        ImpactPreview preview = computePreview(proposed, backlog);

        FormulaChangeProposal proposal = new FormulaChangeProposal();
        proposal.setId(UUID.randomUUID());
        proposal.setTitle(title.trim());
        proposal.setRationale(rationale.trim());
        proposal.setProposedWeights(writeJson(proposed));
        proposal.setCurrentWeights(writeJson(ProposedWeights.current()));
        proposal.setImpactPreview(writeJson(preview));
        proposal.setSampleSize(preview.sampleSize());
        proposal.setPositionsMoved(preview.positionsMoved());
        proposal.setProposedBy(user.getId());
        proposal.setProposedAt(LocalDateTime.now());
        proposal.setStatus(FormulaChangeProposal.Status.PROPOSED);
        proposal = proposalRepository.save(proposal);

        return toView(proposal);
    }

    @Transactional
    public ProposalView decide(UUID proposalId, boolean approve, String note, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        FormulaChangeProposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));

        if (proposal.getStatus() != FormulaChangeProposal.Status.PROPOSED) {
            throw new ConflictException(
                "This proposal was already " + proposal.getStatus().name().toLowerCase(Locale.ROOT)
                    + ". Reopening a decided proposal would erase the record of the decision.");
        }

        proposal.setStatus(approve
            ? FormulaChangeProposal.Status.APPROVED
            : FormulaChangeProposal.Status.REJECTED);
        proposal.setDecidedBy(user.getId());
        proposal.setDecidedAt(LocalDateTime.now());
        proposal.setDecisionNote(note);
        proposal = proposalRepository.save(proposal);

        return toView(proposal);
    }

    @Transactional(readOnly = true)
    public List<ProposalView> list(boolean onlyUndecided) {
        List<FormulaChangeProposal> proposals = onlyUndecided
            ? proposalRepository.findByStatusNewestFirst(FormulaChangeProposal.Status.PROPOSED.name())
            : proposalRepository.findAllNewestFirst();
        return proposals.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProposalView get(UUID proposalId) {
        return proposalRepository.findById(proposalId)
            .map(this::toView)
            .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));
    }

    private ImpactPreview computePreview(ProposedWeights proposed, List<Signal> backlog) {
        // Deterministic ordering under both weightings: score descending, then id. Without the id
        // tiebreak, equal scores would order arbitrarily and the position deltas would be noise.
        List<UUID> currentOrder = rank(backlog, ProposedWeights.current());
        List<UUID> proposedOrder = rank(backlog, proposed);

        java.util.Map<UUID, Integer> currentPosition = new java.util.HashMap<>();
        java.util.Map<UUID, Integer> proposedPosition = new java.util.HashMap<>();
        for (int i = 0; i < currentOrder.size(); i++) {
            currentPosition.put(currentOrder.get(i), i + 1);
        }
        for (int i = 0; i < proposedOrder.size(); i++) {
            proposedPosition.put(proposedOrder.get(i), i + 1);
        }

        List<Mover> movers = new ArrayList<>();
        int moved = 0;
        for (Signal signal : backlog) {
            Integer before = currentPosition.get(signal.getId());
            Integer after = proposedPosition.get(signal.getId());
            if (before == null || after == null || before.equals(after)) {
                continue;
            }
            moved++;
            movers.add(new Mover(
                signal.getId(), signal.getTitle(), before, after, after - before));
        }

        movers.sort(Comparator
            .comparingInt((Mover mover) -> Math.abs(mover.positionDelta())).reversed()
            .thenComparing(Mover::signalId));

        double movedShare = backlog.isEmpty() ? 0.0 : round2((double) moved / backlog.size());

        return new ImpactPreview(
            VERSION,
            ProposedWeights.current().expression(),
            proposed.expression(),
            backlog.size(),
            moved,
            backlog.size() - moved,
            movedShare,
            movers.stream().limit(MAX_MOVERS).toList(),
            interpretation(backlog.size(), moved, movedShare),
            LocalDateTime.now()
        );
    }

    /**
     * Says what the preview is and is not, with every response.
     *
     * <p>A number like "38% of the backlog moves" invites a decider to read it as a forecast. It is
     * not one: changing the formula changes what people report and vote on, so the backlog under the
     * new weights would be a different backlog.
     */
    private String interpretation(int sampleSize, int moved, double movedShare) {
        StringBuilder text = new StringBuilder();
        text.append("This reorders the ").append(sampleSize)
            .append(" signals that exist now: ").append(moved)
            .append(" (").append(Math.round(movedShare * 100))
            .append("%) would change position. It does NOT predict the future. ")
            .append("Changing how signals are scored changes what people report and vote on, so the backlog ")
            .append("under these weights would not be this one. It also does not say whether the change is ")
            .append("right: it shows the mechanical consequence of the numbers, and the judgement is a ")
            .append("separate decision. Approving this proposal does not apply it; applying it means changing ")
            .append("the formula constants, bumping the version, and writing an ADR. ")
            // Equal scores have no meaningful order. The ranking below breaks ties by id so the
            // comparison is deterministic, but a movement that comes only from a tie is arbitrary.
            .append("Signals with equal scores are ordered by id, so a position change caused only by a tie ")
            .append("is arbitrary rather than a real shift in priority.");
        if (sampleSize < 20) {
            text.append(" The sample is only ").append(sampleSize)
                .append(" signals, so small position changes are not meaningful.");
        }
        return text.toString();
    }

    private List<UUID> rank(List<Signal> backlog, ProposedWeights weights) {
        return backlog.stream()
            .sorted(Comparator
                .comparingDouble((Signal signal) -> weights.score(
                    signal.getUrgency(),
                    signal.getImpact(),
                    signal.getAffectedPeople(),
                    signal.getCommunityVotes()))
                .reversed()
                .thenComparing(Signal::getId))
            .map(Signal::getId)
            .toList();
    }

    /** The open backlog, capped so a preview cannot become an unbounded scan. */
    private List<Signal> currentBacklog(Integer limit) {
        int effective = limit == null ? MAX_PREVIEW_SIGNALS : limit;
        if (effective < 1 || effective > MAX_PREVIEW_SIGNALS) {
            throw new IllegalArgumentException(
                "limit must be between 1 and " + MAX_PREVIEW_SIGNALS + ", but was: " + limit);
        }
        return signalRepository.findAll().stream()
            .filter(signal -> signal.getStatus() == null
                || !CLOSED_STATUSES.contains(signal.getStatus().toUpperCase(Locale.ROOT)))
            .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed()
                .thenComparing(Signal::getId))
            .limit(effective)
            .toList();
    }

    private ProposalView toView(FormulaChangeProposal proposal) {
        return new ProposalView(
            proposal.getId(),
            proposal.getTitle(),
            proposal.getRationale(),
            readJson(proposal.getCurrentWeights(), ProposedWeights.class),
            readJson(proposal.getProposedWeights(), ProposedWeights.class),
            readJson(proposal.getImpactPreview(), ImpactPreview.class),
            proposal.getStatus().name(),
            proposal.getProposedBy(),
            proposal.getProposedAt(),
            proposal.getDecidedBy(),
            proposal.getDecidedAt(),
            proposal.getDecisionNote(),
            "Recording an approval does not change the scoring. Applying an approved proposal is a code "
                + "change to PrioritizationFormula, a formula version bump, and an ADR."
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialise the proposal record", ex);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored proposal record is not readable", ex);
        }
    }

    private double round2(double value) {
        return java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .doubleValue();
    }
}