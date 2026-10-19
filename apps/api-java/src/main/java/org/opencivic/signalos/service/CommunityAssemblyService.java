package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.AssemblyDecision;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityAssembly;
import org.opencivic.signalos.domain.ExplainabilitySnapshot;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.AssemblyDecisionRepository;
import org.opencivic.signalos.repository.CommunityAssemblyRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.ExplainabilitySnapshotRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembly mode: what a community deliberated, over which evidence, and what it decided.
 *
 * <p>Two things this deliberately is not:
 *
 * <ul>
 *   <li><b>It is not the official minutes.</b> The platform records what happened in the room; the
 *       authoritative record remains whatever the community is legally required to keep. A platform
 *       that presented its own record as the minutes would be claiming an authority it does not have,
 *       and the response says so on every read.
 *   <li><b>It does not decide anything.</b> It records decisions people made. There is no route that
 *       approves a proposal on the community's behalf.
 * </ul>
 *
 * <p>The evidence link is the part worth getting right. An assembly deliberates over a frozen
 * ranking, and the snapshot from {@link ExplainabilitySnapshotService} is what makes that ranking
 * checkable afterwards. An assembly with no snapshot is allowed — it may be scheduled before the
 * evidence is captured — but the gap is reported rather than hidden, because a decision made over
 * evidence nobody can produce is a decision nobody can review.
 */
@Service
public class CommunityAssemblyService {

    public static final String VERSION = "v1";

    private static final int MAX_MINUTES_LENGTH = 20_000;
    private static final int MAX_RATIONALE_LENGTH = 2_000;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityAssemblyRepository assemblyRepository;
    private final AssemblyDecisionRepository decisionRepository;
    private final ExplainabilitySnapshotRepository snapshotRepository;

    public CommunityAssemblyService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityAssemblyRepository assemblyRepository,
        AssemblyDecisionRepository decisionRepository,
        ExplainabilitySnapshotRepository snapshotRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.assemblyRepository = assemblyRepository;
        this.decisionRepository = decisionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public record CreateAssemblyRequest(
        UUID communityId,
        String title,
        LocalDateTime scheduledFor,
        String location
    ) {}

    public record RecordDecisionRequest(
        UUID subjectId,
        String subjectType,
        AssemblyDecision.DecisionType decision,
        String rationale
    ) {}

    public record DecisionView(
        UUID decisionId,
        UUID subjectId,
        String subjectType,
        String decision,
        String rationale,
        UUID recordedBy,
        LocalDateTime recordedAt
    ) {}

    public record AssemblyView(
        String version,
        UUID assemblyId,
        UUID communityId,
        String title,
        LocalDateTime scheduledFor,
        String location,
        String status,
        UUID snapshotId,
        String snapshotContentHash,
        boolean evidenceCaptured,
        UUID convenedBy,
        LocalDateTime createdAt,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String minutes,
        List<DecisionView> decisions,
        String interpretation
    ) {}

    @Transactional
    public AssemblyView createAssembly(CreateAssemblyRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException(
                "An assembly title is required. An untitled assembly is unfindable when a community "
                    + "holds more than one.");
        }
        if (request.scheduledFor() == null) {
            throw new IllegalArgumentException(
                "A scheduled time is required. An assembly with no date cannot be planned around.");
        }

        CommunityAssembly assembly = new CommunityAssembly();
        assembly.setId(UUID.randomUUID());
        assembly.setCommunityId(community.getId());
        assembly.setTitle(request.title().trim());
        assembly.setScheduledFor(request.scheduledFor());
        assembly.setLocation(request.location() == null || request.location().isBlank()
            ? null : request.location().trim());
        assembly.setConvenedBy(user.getId());
        assembly.setCreatedAt(LocalDateTime.now());
        assembly.setStatus(CommunityAssembly.Status.SCHEDULED);
        assembly = assemblyRepository.save(assembly);

        return view(assembly);
    }

    /**
     * Attaches the frozen ranking the assembly will deliberate over.
     *
     * <p>Separate from creation because the evidence is usually captured closer to the meeting, and
     * forcing it at creation would make people create a placeholder snapshot. A placeholder in an
     * evidence chain is worse than an honest gap.
     */
    @Transactional
    public AssemblyView attachEvidence(UUID assemblyId, UUID communityId, UUID snapshotId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        CommunityAssembly assembly = requireAssembly(assemblyId, communityId);

        if (assembly.getStatus() == CommunityAssembly.Status.CLOSED) {
            throw new ConflictException(
                "This assembly is closed. Attaching evidence afterwards would change what the record "
                    + "says was deliberated over.");
        }

        ExplainabilitySnapshot snapshot = snapshotRepository.findByIdAndCommunityId(snapshotId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Snapshot not found for this community: " + snapshotId));

        assembly.setSnapshotId(snapshot.getId());
        assembly = assemblyRepository.save(assembly);
        return view(assembly);
    }

    @Transactional
    public AssemblyView openAssembly(UUID assemblyId, UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        CommunityAssembly assembly = requireAssembly(assemblyId, communityId);

        if (assembly.getStatus() == CommunityAssembly.Status.CLOSED) {
            throw new ConflictException("This assembly is already closed.");
        }
        if (assembly.getStatus() == CommunityAssembly.Status.OPEN) {
            throw new ConflictException("This assembly is already open.");
        }
        assembly.setStatus(CommunityAssembly.Status.OPEN);
        assembly.setOpenedAt(LocalDateTime.now());
        assembly = assemblyRepository.save(assembly);
        return view(assembly);
    }

    @Transactional
    public AssemblyView closeAssembly(UUID assemblyId, UUID communityId, String minutes, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        CommunityAssembly assembly = requireAssembly(assemblyId, communityId);

        if (assembly.getStatus() == CommunityAssembly.Status.CLOSED) {
            throw new ConflictException(
                "This assembly is already closed. Reopening it would erase the record of when it ended.");
        }
        if (minutes != null && minutes.length() > MAX_MINUTES_LENGTH) {
            throw new IllegalArgumentException(
                "minutes must be at most " + MAX_MINUTES_LENGTH + " characters, but was: " + minutes.length());
        }

        assembly.setStatus(CommunityAssembly.Status.CLOSED);
        assembly.setClosedAt(LocalDateTime.now());
        if (minutes != null && !minutes.isBlank()) {
            assembly.setMinutes(minutes.trim());
        }
        assembly = assemblyRepository.save(assembly);
        return view(assembly);
    }

    @Transactional
    public AssemblyView recordDecision(
        UUID assemblyId,
        UUID communityId,
        RecordDecisionRequest request,
        String username
    ) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        CommunityAssembly assembly = requireAssembly(assemblyId, communityId);

        if (assembly.getStatus() == CommunityAssembly.Status.CLOSED) {
            throw new ConflictException(
                "This assembly is closed. A decision recorded afterwards would appear in the record as "
                    + "having been made in the room.");
        }
        if (request.decision() == null) {
            throw new IllegalArgumentException(
                "A decision type is required: APPROVED, REJECTED, DEFERRED or NOTED.");
        }
        if (request.rationale() == null || request.rationale().isBlank()) {
            // A decision nobody explained cannot be reviewed later, which is the whole reason for
            // recording it.
            throw new IllegalArgumentException(
                "A rationale is required. A decision nobody explained cannot be reviewed afterwards.");
        }
        if (request.rationale().length() > MAX_RATIONALE_LENGTH) {
            throw new IllegalArgumentException(
                "rationale must be at most " + MAX_RATIONALE_LENGTH + " characters, but was: "
                    + request.rationale().length());
        }

        AssemblyDecision decision = new AssemblyDecision();
        decision.setId(UUID.randomUUID());
        decision.setAssemblyId(assembly.getId());
        decision.setSubjectId(request.subjectId());
        decision.setSubjectType(request.subjectType() == null || request.subjectType().isBlank()
            ? null : request.subjectType().trim());
        decision.setDecision(request.decision());
        decision.setRationale(request.rationale().trim());
        decision.setRecordedBy(user.getId());
        decision.setRecordedAt(LocalDateTime.now());
        decisionRepository.save(decision);

        return view(assembly);
    }

    @Transactional(readOnly = true)
    public AssemblyView getAssembly(UUID assemblyId, UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return view(requireAssembly(assemblyId, communityId));
    }

    @Transactional(readOnly = true)
    public List<AssemblyView> listAssemblies(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return assemblyRepository.findByCommunityIdOrderByScheduledForDesc(communityId).stream()
            .map(this::view)
            .toList();
    }

    private CommunityAssembly requireAssembly(UUID assemblyId, UUID communityId) {
        return assemblyRepository.findByIdAndCommunityId(assemblyId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Assembly not found: " + assemblyId));
    }

    private AssemblyView view(CommunityAssembly assembly) {
        List<DecisionView> decisions = new ArrayList<>();
        for (AssemblyDecision decision : decisionRepository.findByAssemblyIdOrderByRecordedAtAsc(assembly.getId())) {
            decisions.add(new DecisionView(
                decision.getId(),
                decision.getSubjectId(),
                decision.getSubjectType(),
                decision.getDecision().name(),
                decision.getRationale(),
                decision.getRecordedBy(),
                decision.getRecordedAt()
            ));
        }

        String snapshotHash = null;
        if (assembly.getSnapshotId() != null) {
            snapshotHash = snapshotRepository.findById(assembly.getSnapshotId())
                .map(ExplainabilitySnapshot::getContentHash)
                .orElse(null);
        }
        boolean evidenceCaptured = assembly.getSnapshotId() != null && snapshotHash != null;

        return new AssemblyView(
            VERSION,
            assembly.getId(),
            assembly.getCommunityId(),
            assembly.getTitle(),
            assembly.getScheduledFor(),
            assembly.getLocation(),
            assembly.getStatus().name(),
            assembly.getSnapshotId(),
            snapshotHash,
            evidenceCaptured,
            assembly.getConvenedBy(),
            assembly.getCreatedAt(),
            assembly.getOpenedAt(),
            assembly.getClosedAt(),
            assembly.getMinutes(),
            decisions,
            interpretation(assembly, evidenceCaptured, decisions.size())
        );
    }

    /**
     * Says what this record is and is not, on every read.
     *
     * <p>Two claims a reader could wrongly take: that this is the official minutes, and that a
     * decision made without captured evidence is reviewable. Both are stated rather than implied.
     */
    private String interpretation(CommunityAssembly assembly, boolean evidenceCaptured, int decisions) {
        StringBuilder text = new StringBuilder();
        text.append("This is the platform's record of what the assembly deliberated and decided. It is ")
            .append("NOT the official minutes: the authoritative record remains whatever the community ")
            .append("is legally required to keep. The platform records decisions people made; it does ")
            .append("not make them.");

        if (!evidenceCaptured) {
            text.append(" No evidence snapshot is attached, so the ranking this assembly deliberated ")
                .append("over cannot be reproduced. A decision made over evidence nobody can produce is ")
                .append("a decision nobody can review.");
        } else {
            text.append(" The ranking deliberated over is frozen and its content hash is recorded, so ")
                .append("what was seen can be checked afterwards.");
        }

        if (decisions == 0) {
            text.append(" No decisions have been recorded yet.");
        }
        return text.toString();
    }
}