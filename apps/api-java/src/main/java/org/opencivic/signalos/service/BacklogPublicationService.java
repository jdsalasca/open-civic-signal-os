package org.opencivic.signalos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityBacklogPublication;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityBacklogPublicationRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotCreateRequest;
import org.opencivic.signalos.web.dto.ExplainabilitySnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes the public backlog as a named, hash-anchored artifact, and lets anyone check it.
 *
 * <p>An explainability snapshot freezes what an assembly saw. This answers the different question a
 * visitor has: <em>is what I am looking at now the thing that was published?</em> Without a recorded
 * ordering hash there is nothing to compare against, and a ranking could shift under visitors
 * silently.
 *
 * <p>One design decision worth stating, because the obvious alternative is wrong: the hash covers
 * the <b>set of items and their scores, not the order they appear in</b>. Rows with equal scores have
 * no defined order — the public endpoint sorts by score alone, so ties come back in whatever order
 * the database chooses. Hashing the literal order would fail verification for a reason that means
 * nothing, and a check that cries wolf is worse than no check. Two lists are the same published
 * backlog if they contain the same signals at the same scores.
 */
@Service
public class BacklogPublicationService {

    public static final String VERSION = "v1";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final CommunityBacklogPublicationRepository publicationRepository;
    private final ExplainabilitySnapshotService snapshotService;

    public BacklogPublicationService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        CommunityBacklogPublicationRepository publicationRepository,
        ExplainabilitySnapshotService snapshotService
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.publicationRepository = publicationRepository;
        this.snapshotService = snapshotService;
    }

    /** A publication plus whether the live ranking still matches it. */
    public record BacklogPublicationView(
        String version,
        UUID publicationId,
        UUID communityId,
        UUID snapshotId,
        String label,
        String orderingHash,
        String formulaVersion,
        int itemCount,
        UUID publishedBy,
        LocalDateTime publishedAt,
        boolean current,
        Boolean matchesLiveRanking,
        String mismatchExplanation
    ) {}

    public record Verification(boolean matches, String publishedHash, String liveHash, String explanation) {}

    /** Publishing is a moderation act: it declares what the public sees. */
    public record PublishRequest(UUID communityId, String label, Integer limit) {}

    @Transactional
    public BacklogPublicationView publish(PublishRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));

        if (request.label() == null || request.label().isBlank()) {
            throw new IllegalArgumentException(
                "A publication label is required. An unlabelled publication is unfindable when someone "
                    + "asks which version they were looking at.");
        }

        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                "limit must be between 1 and " + MAX_LIMIT + ", but was: " + limit);
        }

        List<Signal> ranked = liveRanking(community.getId(), limit);

        // The frozen copy, so the ranking itself is recoverable and tamper-evident.
        ExplainabilitySnapshotResponse snapshot = snapshotService.create(
            new ExplainabilitySnapshotCreateRequest(
                community.getId(), "Published backlog: " + request.label().trim(), limit),
            username);

        String orderingHash = orderingHash(ranked);
        String formulaVersion = snapshot.formulaVersion();

        publicationRepository.findByCommunityIdAndCurrentSlotIsNotNull(community.getId())
            .ifPresent(previous -> {
                previous.setCurrent(false);
                publicationRepository.save(previous);
            });

        CommunityBacklogPublication publication = new CommunityBacklogPublication();
        publication.setId(UUID.randomUUID());
        publication.setCommunityId(community.getId());
        publication.setSnapshotId(snapshot.snapshotId());
        publication.setLabel(request.label().trim());
        publication.setOrderingHash(orderingHash);
        publication.setFormulaVersion(formulaVersion);
        publication.setItemCount(ranked.size());
        publication.setPublishedBy(user.getId());
        publication.setPublishedAt(LocalDateTime.now());
        publication.setCurrent(true);
        publication = publicationRepository.save(publication);

        return toView(publication, true, null);
    }

    /**
     * The current publication, with a live comparison. Public: this is the claim a visitor checks.
     */
    @Transactional(readOnly = true)
    public Optional<BacklogPublicationView> current(UUID communityId) {
        return publicationRepository.findByCommunityIdAndCurrentSlotIsNotNull(communityId)
            .map(publication -> {
                Verification verification = verifyAgainst(publication, true);
                return toView(publication, verification.matches(), verification.explanation());
            });
    }

    /** Recompute the live ordering hash and compare it with what was published. */
    @Transactional(readOnly = true)
    public Verification verify(UUID communityId) {
        CommunityBacklogPublication publication = publicationRepository
            .findByCommunityIdAndCurrentSlotIsNotNull(communityId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No backlog has been published for community " + communityId));
        return verifyAgainst(publication, false);
    }

    @Transactional(readOnly = true)
    public List<BacklogPublicationView> history(UUID communityId) {
        return publicationRepository.findByCommunityIdOrderByPublishedAtDesc(communityId).stream()
            .map(publication -> toView(publication, null, null))
            .toList();
    }

    private Verification verifyAgainst(CommunityBacklogPublication publication, boolean forPublicRead) {
        List<Signal> live = liveRanking(publication.getCommunityId(), publication.getItemCount());
        String liveHash = orderingHash(live);
        boolean matches = liveHash.equalsIgnoreCase(publication.getOrderingHash());

        if (matches) {
            return new Verification(true, publication.getOrderingHash(), liveHash,
                "The live backlog matches what was published: same signals, same scores.");
        }

        // Say what changed rather than only that something did. "It differs" is not actionable.
        String explanation = "The live backlog NO LONGER matches the published one. "
            + "Either signals were added, removed, or rescored since publication. "
            + "This is not proof of tampering: a new report or a vote legitimately changes the ranking. "
            + "It does mean the published hash is no longer a description of what a visitor sees, so "
            + "either publish again or state which version is authoritative."
            + (forPublicRead ? "" : " Published item count: " + publication.getItemCount() + ".");
        return new Verification(false, publication.getOrderingHash(), liveHash, explanation);
    }

    /**
     * The same ordering the public endpoint serves: unresolved, highest score first. Ties are broken
     * by id so this computation is deterministic, which the hash requires even though the endpoint
     * itself does not break ties.
     */
    private List<Signal> liveRanking(UUID communityId, int limit) {
        return signalRepository.findByCommunityId(communityId).stream()
            .filter(signal -> signal.getStatus() == null
                || !SignalStatus.isSettled(signal.getStatus()))
            .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed()
                .thenComparing(Signal::getId))
            .limit(limit)
            .toList();
    }

    /**
     * Hash over the canonical set of {@code id:score} pairs, sorted.
     *
     * <p>Sorting the lines is the point: rows with equal scores have no defined order, so hashing
     * their literal order would fail verification for a reason that means nothing. Membership and
     * scores are what "the same published backlog" means.
     */
    static String orderingHash(List<Signal> ranked) {
        List<String> lines = new ArrayList<>();
        for (Signal signal : ranked) {
            lines.add(signal.getId() + ":" + signal.getPriorityScore());
        }
        lines.sort(Comparator.naturalOrder());
        return sha256(String.join("\n", lines));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable, cannot anchor a publication", ex);
        }
    }

    private BacklogPublicationView toView(
        CommunityBacklogPublication publication,
        Boolean matches,
        String explanation
    ) {
        return new BacklogPublicationView(
            VERSION,
            publication.getId(),
            publication.getCommunityId(),
            publication.getSnapshotId(),
            publication.getLabel(),
            publication.getOrderingHash(),
            publication.getFormulaVersion(),
            publication.getItemCount(),
            publication.getPublishedBy(),
            publication.getPublishedAt(),
            publication.isCurrent(),
            matches,
            explanation
        );
    }
}