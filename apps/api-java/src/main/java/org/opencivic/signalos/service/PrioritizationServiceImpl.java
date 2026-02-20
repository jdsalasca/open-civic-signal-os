package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.Vote;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrioritizationServiceImpl implements PrioritizationService {

    private static final Logger log = LoggerFactory.getLogger(PrioritizationServiceImpl.class);
    private final SignalRepository signalRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    public PrioritizationServiceImpl(SignalRepository signalRepository, VoteRepository voteRepository, UserRepository userRepository) {
        this.signalRepository = signalRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable) {
        return getPrioritizedSignals(pageable, null);
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable, UUID communityId) {
        Page<Signal> basePage = communityId == null
            ? signalRepository.findByStatusNotIn(List.of("FLAGGED", "REJECTED"), pageable)
            : signalRepository.findByStatusNotInAndCommunityId(List.of("FLAGGED", "REJECTED"), communityId, pageable);
        return basePage
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)));
    }

    @Override
    public List<Signal> getTopUnresolved(int limit) {
        return getTopUnresolved(limit, null);
    }

    @Override
    public List<Signal> getTopUnresolved(int limit, UUID communityId) {
        List<Signal> baseSignals = communityId == null
            ? signalRepository.findTopSignalsByStatus("NEW", PageRequest.of(0, limit))
            : signalRepository.findTopSignalsByStatusAndCommunityId("NEW", communityId, PageRequest.of(0, limit));
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
        return new ScoreBreakdown(
            signal.getUrgency() * 30.0,
            signal.getImpact() * 25.0,
            Math.min(signal.getAffectedPeople() / 10.0, 30.0),
            Math.min(signal.getCommunityVotes() / 5.0, 15.0)
        );
    }

    @Override
    public Map<UUID, List<Signal>> findDuplicates() {
        log.info("Starting optimized deduplication search window (last 100 signals)");
        List<Signal> signals = signalRepository.findTopSignalsByStatus("NEW", PageRequest.of(0, 100));
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

    private boolean isSimilar(Signal s1, Signal s2) {
        String t1 = s1.getTitle().toLowerCase();
        String t2 = s2.getTitle().toLowerCase();
        return t1.contains(t2) || t2.contains(t1) || levenshteinDistance(t1, t2) < 5;
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
        // P1-B: Paginated DB-level query
        return signalRepository.findByStatus("FLAGGED", pageable)
                .map(s -> s.withScore(calculateScore(s), getBreakdown(s)));
    }

    @Override
    @Transactional
    public Signal moderateSignal(UUID id, String action, String reason) {
        log.info("Moderating signal {}. Action: {}, Reason: {}", id, action, reason);
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found for moderation: " + id));
        
        if ("APPROVE".equalsIgnoreCase(action)) {
            signal.setStatus(SignalStatus.NEW.name());
        } else {
            signal.setStatus(SignalStatus.REJECTED.name());
        }
        signal.setModerationReason(reason);
        return signalRepository.save(signal);
    }

    @Override
    @Transactional
    public Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String username) {
        return createSignal(title, description, category, urgency, impact, affectedPeople, username, null);
    }

    @Override
    @Transactional
    public Signal createSignal(
        String title,
        String description,
        String category,
        int urgency,
        int impact,
        int affectedPeople,
        String username,
        UUID communityId
    ) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Author user not found: " + username));

        Signal signal = new Signal(
            UUID.randomUUID(), title, description, category,
            urgency, impact, affectedPeople,
            0, 0.0, null, SignalStatus.NEW.name(), new ArrayList<>(), author.getId(), java.time.LocalDateTime.now(), communityId
        );
        
        // Ensure breakdown is calculated and persisted
        ScoreBreakdown breakdown = getBreakdown(signal);
        double score = breakdown.urgency() + breakdown.impact() + breakdown.affectedPeople() + breakdown.communityVotes();
        
        signal.setScoreBreakdown(breakdown);
        signal.setPriorityScore(score);
        
        return saveSignal(signal);
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
        return updateStatus(id, newStatus, null);
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId) {
        log.info("Updating status for signal {}. Target: {}", id, newStatus);
        Signal signal = (communityId == null
            ? signalRepository.findById(id)
            : signalRepository.findByIdAndCommunityId(id, communityId))
                .orElseThrow(() -> new ResourceNotFoundException("Signal with ID " + id + " not found."));

        SignalStatus current = SignalStatus.valueOf(signal.getStatus());
        SignalStatus target = SignalStatus.valueOf(newStatus);
        
        if (!current.canTransitionTo(target)) {
            log.error("Invalid status transition rejected: {} -> {}", current, target);
            throw new ConflictException("Invalid status transition from " + current + " to " + target);
        }
        
        signal.setStatus(target.name());
        return Optional.of(signalRepository.save(signal));
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
