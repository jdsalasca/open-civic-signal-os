package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.ExplainabilitySnapshot;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.ExplainabilitySnapshotRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotCreateRequest;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotResponse;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotVerificationResponse;
import org.opencivic.signalos.web.dto.TrustPacket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explainability snapshots for assemblies: a frozen, self-verifying copy of what a ranked list
 * looked like at one moment.
 *
 * <p>A live trust packet is not enough for a deliberation. An assembly ranks a list on Tuesday,
 * votes on Wednesday, and the record of what they saw has to still be checkable in six months.
 * But a live score moves: one more vote changes it, so "the list we voted on" quietly stops
 * existing.
 *
 * <p>The snapshot freezes the rows and stamps them with a SHA-256 over the canonical payload. A
 * reader can recompute that hash and prove nobody edited the rows afterwards. That is the whole
 * point: minutes in a council session are a factual claim, and this makes the claim checkable
 * rather than merely asserted.
 */
@Service
public class ExplainabilitySnapshotService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final ExplainabilitySnapshotRepository snapshotRepository;
    private final PrioritizationService prioritizationService;
    private final PrioritizationFormulaService formulaService;
    private final ObjectMapper objectMapper;

    public ExplainabilitySnapshotService(
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        ExplainabilitySnapshotRepository snapshotRepository,
        PrioritizationService prioritizationService,
        PrioritizationFormulaService formulaService,
        ObjectMapper objectMapper
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.snapshotRepository = snapshotRepository;
        this.prioritizationService = prioritizationService;
        this.formulaService = formulaService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExplainabilitySnapshotResponse create(ExplainabilitySnapshotCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));

        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                "limit must be between 1 and " + MAX_LIMIT + ", but was: " + limit);
        }

        List<Signal> ranked = signalRepository.findByCommunityId(community.getId()).stream()
            .sorted(java.util.Comparator.comparingDouble(Signal::getPriorityScore).reversed()
                .thenComparing(Signal::getId))
            .limit(limit)
            .toList();

        String formulaVersion = formulaService.getFormula().version();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("communityId", community.getId().toString());
        payload.put("communityName", community.getName());
        payload.put("label", request.label().trim());
        payload.put("formulaVersion", formulaVersion);
        payload.put("formula", formulaService.getFormula().formula());

        ArrayNode entries = payload.putArray("entries");
        for (int position = 1; position <= ranked.size(); position++) {
            Signal signal = ranked.get(position - 1);
            TrustPacket packet = prioritizationService.getTrustPacket(signal.getId());
            ObjectNode entry = entries.addObject();
            entry.put("position", position);
            entry.put("signalId", signal.getId().toString());
            entry.put("title", signal.getTitle());
            entry.put("status", signal.getStatus());
            entry.put("category", signal.getCategory());
            entry.put("finalScore", round(packet.finalScore()));
            entry.put("scoreUrgency", packet.scoreBreakdown().urgency());
            entry.put("scoreImpact", packet.scoreBreakdown().impact());
            entry.put("scorePeople", packet.scoreBreakdown().affectedPeople());
            entry.put("scoreVotes", packet.scoreBreakdown().communityVotes());
            // Per-row verification hash, so one disputed entry can be checked without
            // re-deriving the whole snapshot.
            entry.put("entryVerificationHash", packet.verificationHash());
        }

        String canonical = canonicalise(payload);
        String contentHash = sha256(canonical);

        ExplainabilitySnapshot snapshot = new ExplainabilitySnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setCommunityId(community.getId());
        snapshot.setLabel(request.label().trim());
        snapshot.setCreatedBy(user.getId());
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setFormulaVersion(formulaVersion);
        snapshot.setEntryCount(ranked.size());
        snapshot.setPayload(canonical);
        snapshot.setContentHash(contentHash);
        snapshot.setVerifiedAt(LocalDateTime.now());
        snapshot = snapshotRepository.save(snapshot);

        return toResponse(snapshot, community.getName(), true, null);
    }

    @Transactional(readOnly = true)
    public List<ExplainabilitySnapshotResponse> list(UUID communityId, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        if (!communityRepository.existsById(communityId)) {
            throw new ResourceNotFoundException("Community not found: " + communityId);
        }
        requireMembership(user, communityId);
        return snapshotRepository.findByCommunityIdOrderByCreatedAtDescIdDesc(communityId).stream()
            .map(snapshot -> toResponse(snapshot, null, null, null))
            .toList();
    }

    @Transactional(readOnly = true)
    public ExplainabilitySnapshotResponse get(UUID snapshotId, UUID communityId, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        requireMembership(user, communityId);
        ExplainabilitySnapshot snapshot = snapshotRepository.findByIdAndCommunityId(snapshotId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Explainability snapshot not found: " + snapshotId));
        return toResponse(snapshot, null, verify(snapshot), null);
    }

    /**
     * Recompute the hash over the stored payload and compare.
     *
     * <p>This deliberately does not regenerate the list from current data. The point is to prove
     * the stored rows were not altered; regenerating would answer a different question, and would
     * return "valid" for a snapshot whose original ranking has since changed, which is not
     * tampering but is not what a reader of the minutes is asking.
     */
    @Transactional(readOnly = true)
    public ExplainabilitySnapshotVerificationResponse verify(
        UUID snapshotId,
        UUID communityId,
        String username
    ) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        requireMembership(user, communityId);
        ExplainabilitySnapshot snapshot = snapshotRepository.findByIdAndCommunityId(snapshotId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Explainability snapshot not found: " + snapshotId));

        String recomputed = sha256(canonicalise(readTree(snapshot.getPayload())));
        boolean valid = recomputed.equalsIgnoreCase(snapshot.getContentHash());

        return new ExplainabilitySnapshotVerificationResponse(
            snapshot.getId(),
            snapshot.getContentHash(),
            recomputed,
            valid,
            valid
                ? "Stored rows match their recorded hash."
                : "Stored rows do NOT match their recorded hash. This snapshot was modified after it "
                    + "was created and should not be cited as evidence."
        );
    }

    private Boolean verify(ExplainabilitySnapshot snapshot) {
        return sha256(canonicalise(readTree(snapshot.getPayload())))
            .equalsIgnoreCase(snapshot.getContentHash());
    }

/**
 * A snapshot is assembly evidence, so reading one requires community membership, same as any
 * other community-scoped read. Not optional: an unauthenticated caller must not be able to
 * enumerate another community's deliberation record.
 */
private void requireMembership(User user, UUID communityId) {
    if (!communityRepository.existsById(communityId)) {
        throw new ResourceNotFoundException("Community not found: " + communityId);
    }
    if (membershipRepository.findByUserIdAndCommunityId(user.getId(), communityId).isEmpty()) {
        throw new UnauthorizedActionException("User is not a member of community " + communityId);
    }
}

    private ExplainabilitySnapshotResponse toResponse(
        ExplainabilitySnapshot snapshot,
        String communityName,
        Boolean verified,
        String verificationNote
    ) {
        return new ExplainabilitySnapshotResponse(
            snapshot.getId(),
            snapshot.getCommunityId(),
            communityName,
            snapshot.getLabel(),
            snapshot.getCreatedBy(),
            snapshot.getCreatedAt(),
            snapshot.getFormulaVersion(),
            snapshot.getEntryCount(),
            snapshot.getContentHash(),
            verified,
            verificationNote,
            snapshot.getPayload()
        );
    }

    private com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored snapshot payload is not readable JSON", ex);
        }
    }

    /**
     * Canonical JSON: object keys sorted at every level.
     *
     * <p>Without this the hash would depend on field ordering, which depends on how the map was
     * built, and a snapshot could fail verification for reasons that have nothing to do with
     * whether anyone tampered with it.
     */
    public String canonicalise(com.fasterxml.jackson.databind.JsonNode node) {
        try {
            return objectMapper.writeValueAsString(sortKeys(node));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not canonicalise snapshot payload", ex);
        }
    }

    private com.fasterxml.jackson.databind.JsonNode sortKeys(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            java.util.List<String> names = new java.util.ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(java.util.Comparator.naturalOrder());
            ObjectNode ordered = objectMapper.createObjectNode();
            for (String name : names) {
                ordered.set(name, sortKeys(node.get(name)));
            }
            return ordered;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            node.forEach(child -> copy.add(sortKeys(child)));
            return copy;
        }
        return node;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable, cannot seal a snapshot", ex);
        }
    }

    private double round(double value) {
        return java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .doubleValue();
    }
}