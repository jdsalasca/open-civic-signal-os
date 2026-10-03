package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.PrioritizationFormula;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalSourceChannel;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.Vote;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.repository.VoteRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.web.dto.TrustPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrioritizationServiceImpl implements PrioritizationService {

    private static final Logger log = LoggerFactory.getLogger(PrioritizationServiceImpl.class);

    /**
     * Stamped on every signal so an auditor can tell which scoring rule produced a rank.
     * Bump this string whenever the weights in {@link #getBreakdown} change, and add a
     * regression note in docs/ per the reproducibility rule.
     */
    public static final String TRANSFORMATION_VERSION = Signal.TRANSFORMATION_VERSION_V1;

    /**
     * Statuses hidden from the public ranking regardless of any filter.
     *
     * <p>A threshold is not allowed to bring back content moderation removed, so every path that
     * does not name statuses explicitly still excludes these.
     */
    static final List<String> HIDDEN_STATUSES = List.of("FLAGGED", "REJECTED");

    private final SignalRepository signalRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final SignalStatusEntryRepository statusHistoryRepository;

    public PrioritizationServiceImpl(SignalRepository signalRepository, 
                                  VoteRepository voteRepository, 
                                  UserRepository userRepository,
                                  SignalStatusEntryRepository statusHistoryRepository) {
        this.signalRepository = signalRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Override
    public TrustPacket getTrustPacket(UUID signalId) {
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + signalId));
        
        ScoreBreakdown breakdown = getBreakdown(signal);
        double score = calculateScore(signal);
        
        String rawData = signal.getId() + ":" + signal.getCreatedAt() + ":" + score;
        String hash = generateHash(rawData);

        return new TrustPacket(
            signal.getId(),
            signal.getTitle(),
            signal.getStatus(),
            signal.getCreatedAt(),
            score,
            breakdown,
            PrioritizationFormula.expression(),
            hash
        );
    }

    @Override
    public List<SignalStatusEntry> getStatusHistory(UUID signalId) {
        return statusHistoryRepository.findBySignalIdOrderByCreatedAtDesc(signalId);
    }

    @Override
    @Transactional
    public Signal assignSignal(UUID signalId, String assigneeUsername, String changedBy, String reason) {
        Signal signal = signalRepository.findById(signalId)
            .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + signalId));

        User assignee = userRepository.findByUsername(assigneeUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found: " + assigneeUsername));

        signal.setAssignedToUserId(assignee.getId());
        Signal saved = signalRepository.save(signal);

        statusHistoryRepository.save(new SignalStatusEntry(
            signalId,
            signal.getStatus(),
            signal.getStatus(),
            "ASSIGNED",
            changedBy,
            reason == null || reason.isBlank() ? "Case assignment updated" : reason,
            assignee.getUsername()
        ));

        return saved;
    }

    /**
     * Unknown channel strings are rejected rather than silently defaulted, so a bad ingest
     * label shows up as a failed import instead of an unattributed civic signal.
     */
    private SignalSourceChannel resolveSourceChannel(String rawChannel) {
        if (rawChannel == null || rawChannel.isBlank()) {
            return SignalSourceChannel.WEB_FORM;
        }
        try {
            return SignalSourceChannel.valueOf(rawChannel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown signal sourceChannel: " + rawChannel);
        }
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable) {
        return getPrioritizedSignals(pageable, null, null);
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable, UUID communityId) {
        return getPrioritizedSignals(pageable, communityId, null);
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable, UUID communityId, Collection<String> statuses) {
        return getPrioritizedSignals(pageable, communityId, statuses, null);
    }

    @Override
    public Page<Signal> getPrioritizedSignals(
        Pageable pageable,
        UUID communityId,
        Collection<String> statuses,
        Double minScore
    ) {
        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        boolean hasThreshold = minScore != null;

        Page<Signal> basePage;
        if (hasThreshold && hasStatuses && communityId == null) {
            basePage = signalRepository.findByStatusInAndPriorityScoreGreaterThanEqual(statuses, minScore, pageable);
        } else if (hasThreshold && hasStatuses) {
            basePage = signalRepository.findByStatusInAndCommunityIdAndPriorityScoreGreaterThanEqual(statuses, communityId, minScore, pageable);
        } else if (hasThreshold && communityId == null) {
            // Keeps excluding FLAGGED and REJECTED, same as the unfiltered path.
            basePage = signalRepository.findByStatusNotInAndPriorityScoreGreaterThanEqual(
                HIDDEN_STATUSES, minScore, pageable);
        } else if (hasThreshold) {
            basePage = signalRepository.findByStatusNotInAndCommunityIdAndPriorityScoreGreaterThanEqual(
                HIDDEN_STATUSES, communityId, minScore, pageable);
        } else if (hasStatuses && communityId == null) {
            basePage = signalRepository.findByStatusIn(statuses, pageable);
        } else if (hasStatuses) {
            basePage = signalRepository.findByStatusInAndCommunityId(statuses, communityId, pageable);
        } else if (communityId == null) {
            basePage = signalRepository.findByStatusNotIn(HIDDEN_STATUSES, pageable);
        } else {
            basePage = signalRepository.findByStatusNotInAndCommunityId(HIDDEN_STATUSES, communityId, pageable);
        }
        return basePage
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)));
    }

    @Override
    public List<Signal> getTopUnresolved(int limit) {
        return getTopUnresolved(limit, null);
    }

    @Override
    public List<Signal> getTopUnresolved(int limit, UUID communityId) {
        return getTopUnresolved(limit, communityId, null);
    }

    @Override
    public List<Signal> getTopUnresolved(int limit, UUID communityId, Double minScore) {
        List<Signal> baseSignals;
        if (minScore != null) {
            PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "priorityScore"));
            baseSignals = communityId == null
                ? signalRepository.findByStatusAndPriorityScoreGreaterThanEqual("NEW", minScore, page).getContent()
                : signalRepository.findByStatusAndCommunityIdAndPriorityScoreGreaterThanEqual("NEW", communityId, minScore, page).getContent();
        } else {
            baseSignals = communityId == null
                ? signalRepository.findTopSignalsByStatus("NEW", PageRequest.of(0, limit))
                : signalRepository.findTopSignalsByStatusAndCommunityId("NEW", communityId, PageRequest.of(0, limit));
        }
        return baseSignals
                .stream()
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Signal> getSignalById(UUID id) {
        return getSignalById(id, null);
    }

    @Override
    public Optional<Signal> getSignalById(UUID id, UUID communityId) {
        Optional<Signal> baseSignal = communityId == null
            ? signalRepository.findById(id)
            : signalRepository.findByIdAndCommunityId(id, communityId);
        return baseSignal
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)));
    }

    @Override
    public double calculateScore(Signal signal) {
        ScoreBreakdown b = getBreakdown(signal);
        return b.urgency() + b.impact() + b.affectedPeople() + b.communityVotes();
    }

    @Override
    public ScoreBreakdown getBreakdown(Signal signal) {
        // Every term comes from PrioritizationFormula, which is also what the published formula
        // string is built from. The numbers used to live here and again as prose in two other
        // classes; a divergence is no longer possible by construction.
        return new ScoreBreakdown(
            PrioritizationFormula.urgencyTerm(signal.getUrgency()),
            PrioritizationFormula.impactTerm(signal.getImpact()),
            PrioritizationFormula.affectedPeopleTerm(signal.getAffectedPeople()),
            PrioritizationFormula.communityVotesTerm(signal.getCommunityVotes())
        );
    }

    @Override
    public Map<UUID, List<Signal>> findDuplicates() {
        return findDuplicates(null);
    }

    @Override
    public Map<UUID, List<Signal>> findDuplicates(UUID communityId) {
        log.info("Starting scoped deduplication search for community: {}", communityId);
        List<Signal> signals = communityId == null 
            ? signalRepository.findTopSignalsByStatus("NEW", PageRequest.of(0, 100))
            : signalRepository.findTopSignalsByStatusAndCommunityId("NEW", communityId, PageRequest.of(0, 100));
        
        Map<UUID, List<Signal>> duplicateMap = new HashMap<>();
        Set<UUID> processed = new HashSet<>();

        for (int i = 0; i < signals.size(); i++) {
            Signal s1 = signals.get(i);
            if (processed.contains(s1.getId())) continue;

            List<Signal> dups = new ArrayList<>();
            for (int j = i + 1; j < signals.size(); j++) {
                Signal s2 = signals.get(j);
                if (isSimilar(s1, s2)) {
                    dups.add(s2);
                    processed.add(s2.getId());
                }
            }

            if (!dups.isEmpty()) {
                duplicateMap.put(s1.getId(), dups);
                processed.add(s1.getId());
            }
        }
        return duplicateMap;
    }

    /**
     * Similarity between two signals on the same scale the boolean check uses, so the review queue
 * and the merge decision can never disagree about what counted as similar.
 *
 * <p>Shares its components with {@link #isSimilar}: identical and containment cases return 1.0,
 * everything else returns the token overlap the boolean path already gates on. Returning the
 * boolean twice would leave a reviewer unable to see how close a borderline case was.
 */
public double similarityScore(Signal s1, Signal s2) {
    if (s1 == null || s2 == null) {
        return 0.0;
    }
    if (s1.getCategory() == null || s2.getCategory() == null
        || !s1.getCategory().equalsIgnoreCase(s2.getCategory())) {
        return 0.0;
    }
    String t1 = normalizeTitle(s1.getTitle());
    String t2 = normalizeTitle(s2.getTitle());
    if (t1.isBlank() || t2.isBlank()) {
        return 0.0;
    }
    if (t1.equals(t2)) {
        return 1.0;
    }
    int minLen = Math.min(t1.length(), t2.length());
    if (minLen >= 12 && (t1.contains(t2) || t2.contains(t1))) {
        return 1.0;
    }
    return tokenOverlap(t1, t2);
}

    private boolean isSimilar(Signal s1, Signal s2) {
        if (s1.getCategory() == null || s2.getCategory() == null || !s1.getCategory().equalsIgnoreCase(s2.getCategory())) {
            return false;
        }

        String t1 = normalizeTitle(s1.getTitle());
        String t2 = normalizeTitle(s2.getTitle());
        if (t1.isBlank() || t2.isBlank()) {
            return false;
        }
        if (t1.equals(t2)) {
            return true;
        }

        int minLen = Math.min(t1.length(), t2.length());
        if (minLen >= 12 && (t1.contains(t2) || t2.contains(t1))) {
            return true;
        }

        double overlap = tokenOverlap(t1, t2);
        int distance = levenshteinDistance(t1, t2);
        int adaptiveThreshold = Math.max(3, minLen / 5);

        return overlap >= 0.75 || (overlap >= 0.55 && distance <= adaptiveThreshold);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return ascii
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private double tokenOverlap(String t1, String t2) {
        Set<String> a = Arrays.stream(t1.split(" "))
            .filter(token -> token.length() >= 3)
            .collect(Collectors.toSet());
        Set<String> b = Arrays.stream(t2.split(" "))
            .filter(token -> token.length() >= 3)
            .collect(Collectors.toSet());

        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return (double) intersection.size() / (double) Math.min(a.size(), b.size());
    }

    private int levenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] 
                        + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1), 
                        dp[i - 1][j] + 1), dp[i][j - 1] + 1);
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    @Override
    @Transactional
    public Signal mergeSignals(UUID targetId, List<UUID> duplicateIds) {
        log.info("Merging {} signals into target: {}", duplicateIds.size(), targetId);
        if (duplicateIds.contains(targetId)) {
            throw new ConflictException("Target signal cannot be part of its own duplicates list.");
        }

        Signal target = signalRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target signal not found: " + targetId));
        
        for (UUID dupId : duplicateIds) {
            Signal dup = signalRepository.findById(dupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Duplicate signal not found: " + dupId));
            
            target.setCommunityVotes(target.getCommunityVotes() + dup.getCommunityVotes());
            target.getMergedFrom().add(dupId);
            signalRepository.delete(dup);
        }
        
        target.setPriorityScore(calculateScore(target));
        return signalRepository.save(target);
    }

    @Override
    public Page<Signal> getFlaggedSignals(Pageable pageable) {
        return signalRepository.findByStatus("FLAGGED", pageable)
                .map(s -> s.withScore(calculateScore(s), getBreakdown(s)));
    }

    @Override
    @Transactional
    public Signal moderateSignal(UUID id, String action, String reason) {
        log.info("Moderating signal {}. Action: {}, Reason: {}", id, action, reason);
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found for moderation: " + id));
        
        String oldStatus = signal.getStatus();
        if ("APPROVE".equalsIgnoreCase(action)) {
            signal.setStatus(SignalStatus.NEW.name());
        } else {
            signal.setStatus(SignalStatus.REJECTED.name());
        }
        signal.setModerationReason(reason);
        Signal saved = signalRepository.save(signal);

        statusHistoryRepository.save(new SignalStatusEntry(
            id, oldStatus, signal.getStatus(), "STATUS_CHANGED", "moderator", reason, null
        ));

        return saved;
    }

    @Override
    @Transactional
    public Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String username) {
        return createSignal(title, description, category, urgency, impact, affectedPeople, imageUrl, locationLabel, evidenceUrls, latitude, longitude, username, null);
    }

@Override
    @Transactional
    public Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String username, UUID communityId) {
        return createSignal(
            title, description, category, urgency, impact, affectedPeople,
            imageUrl, locationLabel, evidenceUrls, latitude, longitude,
            null, null, username, communityId
        );
    }

    @Override
    @Transactional
    public Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String sourceChannel, String sourceRef, String username, UUID communityId) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Author user not found: " + username));

        List<String> normalizedEvidence = new ArrayList<>();
        if (evidenceUrls != null) {
            normalizedEvidence = evidenceUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        }
        String normalizedPrimaryEvidence = normalizedEvidence.isEmpty()
            ? (imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim())
            : normalizedEvidence.get(0);

        Signal signal = new Signal(
            UUID.randomUUID(), title, description, category,
            urgency, impact, affectedPeople,
            0, 0.0, null, SignalStatus.NEW.name(), new ArrayList<>(), author.getId(), java.time.LocalDateTime.now(), communityId);
signal.setLatitude(latitude);
        signal.setLongitude(longitude);
        signal.setSourceChannel(resolveSourceChannel(sourceChannel));
        signal.setSourceRef(sourceRef == null || sourceRef.isBlank() ? null : sourceRef.trim());
        signal.setTransformationVersion(TRANSFORMATION_VERSION);
        signal.setImageUrl(normalizedPrimaryEvidence);
        signal.setLocationLabel(locationLabel == null || locationLabel.isBlank() ? null : locationLabel.trim());
        signal.setEvidenceUrls(normalizedEvidence);
        
        ScoreBreakdown breakdown = getBreakdown(signal);
        double score = breakdown.urgency() + breakdown.impact() + breakdown.affectedPeople() + breakdown.communityVotes();
        
        signal.setScoreBreakdown(breakdown);
        signal.setPriorityScore(score);
        
        Signal saved = saveSignal(signal);
        
        statusHistoryRepository.save(new SignalStatusEntry(
            saved.getId(), "NONE", "NEW", "CREATED", username, "Initial report submission", null
        ));

        return saved;
    }

    @Override
    public Signal saveSignal(Signal signal) {
        if (signal.getUrgency() == 5 && signal.getAffectedPeople() < 5) {
            log.warn("Auto-flagging signal due to high urgency/low impact ratio: {}", signal.getTitle());
            signal.setStatus(SignalStatus.FLAGGED.name());
            signal.setModerationReason("Suspicious high urgency for very low population. Auto-flagged for review.");
        }
        return signalRepository.save(signal);
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus) {
        return updateStatus(id, newStatus, null, "system_operator", "Standard lifecycle transition");
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus, String changedBy, String reason) {
        return updateStatus(id, newStatus, null, changedBy, reason);
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId) {
        return updateStatus(id, newStatus, communityId, "system_operator", "Standard lifecycle transition");
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId, String changedBy, String reason) {
        log.info("Updating status for signal {}. Target: {}", id, newStatus);
        Signal signal = (communityId == null
            ? signalRepository.findById(id)
            : signalRepository.findByIdAndCommunityId(id, communityId))
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found."));

        String oldStatus = signal.getStatus();
        SignalStatus current = SignalStatus.valueOf(oldStatus);
        SignalStatus target = SignalStatus.valueOf(newStatus);
        
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Invalid transition from " + current + " to " + target);
        }
        
        signal.setStatus(target.name());
        Signal saved = signalRepository.save(signal);
        
        statusHistoryRepository.save(new SignalStatusEntry(
            id,
            oldStatus,
            newStatus,
            "STATUS_CHANGED",
            changedBy == null || changedBy.isBlank() ? "system_operator" : changedBy,
            reason == null || reason.isBlank() ? "Standard lifecycle transition" : reason,
            null
        ));
        
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Signal voteForSignal(UUID signalId, String username) {
        return voteForSignal(signalId, username, null);
    }

    @Override
    @Transactional
    public Signal voteForSignal(UUID signalId, String username, UUID communityId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Signal signal = (communityId == null
            ? signalRepository.findById(signalId)
            : signalRepository.findByIdAndCommunityId(signalId, communityId))
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + signalId));

        if (voteRepository.findByUserIdAndSignalId(user.getId(), signalId).isPresent()) {
            throw new ConflictException("User has already supported this community issue.");
        }

        try {
            voteRepository.save(new Vote(user.getId(), signalId));
            signal.setCommunityVotes(signal.getCommunityVotes() + 1);
            signal.setPriorityScore(calculateScore(signal));
            return signalRepository.save(signal);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Concurrent support attempt detected and rejected.");
        }
    }
}

