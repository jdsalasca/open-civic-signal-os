package org.opencivic.signalos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalMergeDecision;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalMergeDecisionRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.SignalMergeSuggestionResponse;
import org.opencivic.signalos.web.dto.SignalMergeReviewRequest;
import org.opencivic.signalos.web.dto.SignalMergeReviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Human-in-the-loop duplicate clustering.
 *
 * <p>Existing duplicate detection scores pairs by title and description similarity and
 * {@code POST /api/signals/merge} merges them. Both were missing the part that makes such a
 * feature trustworthy: a record of what a person decided, and of what they were shown when they
 * decided it.
 *
 * <p>Without that, a wrong merge is indistinguishable from a correct one afterwards. The merged
 * signal keeps a {@code mergedFrom} list of ids, but nothing says who decided, on what suggestion,
 * at what threshold, or whether they later changed their mind.
 *
 * <p>Three properties this holds:
 *
 * <ul>
 *   <li><b>The suggestion is stored verbatim.</b> A re-run next month with a tweaked threshold
 *       must not rewrite the history of what a reviewer was shown.
 *   <li><b>Declining is a first-class outcome.</b> A review that only records approvals trains
 *       people that the log is a changelog rather than a record, and then they stop reading it.
 *   <li><b>The algorithm never merges on its own.</b> This service records decisions; the merge
 *       itself is what it authorises, and only after a named person asked for it.
 * </ul>
 */
@Service
public class SignalMergeReviewService {

    /** Above this, two reports are near-certainly the same complaint. */
    public static final double DEFAULT_THRESHOLD = 0.75;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final SignalMergeDecisionRepository decisionRepository;
    private final PrioritizationService prioritizationService;
    private final ObjectMapper objectMapper;

    public SignalMergeReviewService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        SignalMergeDecisionRepository decisionRepository,
        PrioritizationService prioritizationService,
        ObjectMapper objectMapper
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.decisionRepository = decisionRepository;
        this.prioritizationService = prioritizationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Suggestions for one community, with the reviewer's outstanding decisions attached.
     *
     * <p>A suggestion that a reviewer already declined is still returned, marked with that
     * decision, rather than silently disappearing. Hiding it would make the queue look
     * unfinished every time someone had correctly said "these are not duplicates".
     */
    @Transactional(readOnly = true)
    public List<SignalMergeSuggestionResponse> getSuggestions(
        UUID communityId,
        Double threshold,
        String username
    ) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        double effective = threshold == null ? DEFAULT_THRESHOLD : threshold;
        if (effective <= 0 || effective > 1) {
            throw new IllegalArgumentException(
                "threshold must be greater than 0 and at most 1, but was: " + threshold);
        }

        Map<UUID, List<SignalMergeSuggestionResponse.SuggestedDuplicate>> clusters =
            new LinkedHashMap<>();
        for (Map.Entry<UUID, List<Signal>> entry
            : prioritizationService.findDuplicates(community.getId()).entrySet()) {
            clusters.put(entry.getKey(), toSuggestions(
                signalRepository.findById(entry.getKey()).orElse(null),
                entry.getValue()));
        }

        Map<UUID, String> latestDecision = new LinkedHashMap<>();
        decisionRepository.findByCommunityIdOrderByDecidedAtDesc(communityId).forEach(decision -> {
            // Ordered newest first, so the first time a target is seen is its latest decision.
            latestDecision.putIfAbsent(decision.getTargetSignalId(), decision.getDecision().name());
        });

        List<SignalMergeSuggestionResponse> out = new ArrayList<>();
        clusters.forEach((targetId, candidates) -> {
            Signal target = signalRepository.findById(targetId).orElse(null);
            if (target == null) {
                return;
            }
            List<SignalMergeSuggestionResponse.SuggestedDuplicate> aboveThreshold = candidates.stream()
                .filter(candidate -> candidate.similarity() >= effective)
                .toList();
            if (aboveThreshold.isEmpty()) {
                return;
            }
            out.add(new SignalMergeSuggestionResponse(
                targetId,
                target.getTitle(),
                target.getCategory(),
                aboveThreshold,
                effective,
                latestDecision.get(targetId),
                null
            ));
        });

        out.sort(Comparator
            .comparingDouble((SignalMergeSuggestionResponse suggestion) ->
                suggestion.candidates().stream().mapToDouble(
                    SignalMergeSuggestionResponse.SuggestedDuplicate::similarity).max().orElse(0))
            .reversed()
            .thenComparing(SignalMergeSuggestionResponse::targetSignalId));

        return out;
    }

    /**
     * Record a decision and, when approved, apply the merge it authorises.
     *
     * <p>One transaction on purpose. A recorded approval with no merge applied would leave the
     * audit trail claiming something that did not happen.
     */
    @Transactional
    public SignalMergeReviewResponse review(SignalMergeReviewRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        Signal target = signalRepository.findById(request.targetSignalId())
            .orElseThrow(() -> new ResourceNotFoundException("Target signal not found: " + request.targetSignalId()));
        Community community = communityRepository.findById(target.getCommunityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + target.getCommunityId()));

        List<SignalMergeSuggestionResponse.SuggestedDuplicate> suggestions =
            request.suggestedSimilarities() == null
                ? List.of()
                : request.suggestedSimilarities();

        SignalMergeDecision.Decision decision = request.decision();

        if (decision == SignalMergeDecision.Decision.APPROVED) {
            if (suggestions.isEmpty()) {
                throw new IllegalArgumentException(
                    "An approval must name the suggestions it accepted, otherwise the merge is unauditable.");
            }
            suggestions.stream()
                .filter(candidate -> candidate.similarity() < request.threshold())
                .findFirst()
                .ifPresent(below -> {
                    throw new IllegalArgumentException(
                        "Cannot approve a suggestion scoring " + below.similarity()
                            + " below the stated threshold " + request.threshold() + ".");
                });
        }

        SignalMergeDecision record = new SignalMergeDecision();
        record.setId(UUID.randomUUID());
        record.setCommunityId(community.getId());
        record.setTargetSignalId(target.getId());
        record.setDecidedBy(user.getId());
        record.setDecidedAt(LocalDateTime.now());
        record.setSimilarityThreshold(request.threshold());
        record.setSuggestedSimilarities(writeJson(suggestions));
        record.setMergedSignalIds(writeJson(
            suggestions.stream().map(SignalMergeSuggestionResponse.SuggestedDuplicate::signalId).toList()));
        record.setDecision(decision);
        record.setNote(request.note());
        record = decisionRepository.save(record);

        UUID mergedTargetId = null;
        String mergedTitle = null;
        if (decision == SignalMergeDecision.Decision.APPROVED) {
            Signal merged = prioritizationService.mergeSignals(
                target.getId(),
                suggestions.stream().map(SignalMergeSuggestionResponse.SuggestedDuplicate::signalId).toList());
            mergedTargetId = merged.getId();
            mergedTitle = merged.getTitle();
        }

        return new SignalMergeReviewResponse(
            record.getId(),
            target.getId(),
            target.getTitle(),
            decision,
            suggestions.size(),
            mergedTargetId,
            mergedTitle,
            record.getDecidedAt(),
            record.getNote()
        );
    }

    @Transactional(readOnly = true)
    public List<SignalMergeReviewResponse> getHistory(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        List<SignalMergeReviewResponse> out = new ArrayList<>();
        for (SignalMergeDecision record : decisionRepository.findByCommunityIdOrderByDecidedAtDesc(communityId)) {
            out.add(new SignalMergeReviewResponse(
                record.getId(),
                record.getTargetSignalId(),
                null,
                record.getDecision(),
                countJsonArray(record.getMergedSignalIds()),
                record.getDecision() == SignalMergeDecision.Decision.APPROVED ? record.getTargetSignalId() : null,
                null,
                record.getDecidedAt(),
                record.getNote()
            ));
        }
        return out;
    }

    private List<SignalMergeSuggestionResponse.SuggestedDuplicate> toSuggestions(
            Signal target,
            List<Signal> duplicates
        ) {
        List<SignalMergeSuggestionResponse.SuggestedDuplicate> out = new ArrayList<>();
        for (Signal duplicate : duplicates) {
            out.add(new SignalMergeSuggestionResponse.SuggestedDuplicate(
                duplicate.getId(),
                duplicate.getTitle(),
                duplicate.getCategory(),
                duplicate.getStatus(),
                duplicate.getCreatedAt(),
                prioritizationService.similarityScore(target, duplicate)
            ));
        }
        out.sort(Comparator.comparing(
            SignalMergeSuggestionResponse.SuggestedDuplicate::signalId));
        return out;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialise the merge record", ex);
        }
    }

    private int countJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {}).size();
        } catch (Exception ex) {
            return 0;
        }
    }
}