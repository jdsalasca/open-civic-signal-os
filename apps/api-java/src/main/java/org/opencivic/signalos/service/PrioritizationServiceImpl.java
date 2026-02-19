package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.Vote;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.repository.VoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrioritizationServiceImpl implements PrioritizationService {

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
        return signalRepository.findByStatusNotIn(List.of("FLAGGED", "REJECTED"), pageable)
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)));
    }

    @Override
    public List<Signal> getTopUnresolved(int limit) {
        // P2-15: Sorting in DB would be better, currently limiting in stream for logic clarity
        return signalRepository.findAll().stream()
                .filter(signal -> "NEW".equals(signal.getStatus()))
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)))
                .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Signal> getSignalById(UUID id) {
        return signalRepository.findById(id)
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
        List<Signal> signals = signalRepository.findAll();
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
        // P1-10: Implementation of actual merge logic
        Signal target = signalRepository.findById(targetId).orElseThrow(() -> new RuntimeException("Target signal not found"));
        for (UUID dupId : duplicateIds) {
            Signal dup = signalRepository.findById(dupId).orElseThrow(() -> new RuntimeException("Duplicate signal " + dupId + " not found"));
            target.setCommunityVotes(target.getCommunityVotes() + dup.getCommunityVotes());
            target.getMergedFrom().add(dupId);
            signalRepository.delete(dup);
        }
        target.setPriorityScore(calculateScore(target));
        return signalRepository.save(target);
    }

    @Override
    public List<Signal> getFlaggedSignals() {
        return signalRepository.findAll().stream()
                .filter(s -> "FLAGGED".equals(s.getStatus()))
                .map(s -> s.withScore(calculateScore(s), getBreakdown(s)))
                .collect(Collectors.toList());
    }

    @Override
    public Signal moderateSignal(UUID id, String action, String reason) {
        Signal signal = signalRepository.findById(id).orElseThrow();
        if ("APPROVE".equalsIgnoreCase(action)) {
            signal.setStatus(SignalStatus.NEW.name());
        } else {
            signal.setStatus(SignalStatus.REJECTED.name());
        }
        signal.setModerationReason(reason);
        return signalRepository.save(signal);
    }

    @Override
    public Signal saveSignal(Signal signal) {
        if (signal.getUrgency() == 5 && signal.getAffectedPeople() < 5) {
            signal.setStatus(SignalStatus.FLAGGED.name());
            signal.setModerationReason("Suspicious high urgency for very low population. Auto-flagged for review.");
        }
        return signalRepository.save(signal);
    }

    @Override
    @Transactional
    public Optional<Signal> updateStatus(UUID id, String newStatus) {
        // P1-9: Status validation through Enum
        return signalRepository.findById(id).map(signal -> {
            SignalStatus current = SignalStatus.valueOf(signal.getStatus());
            SignalStatus target = SignalStatus.valueOf(newStatus);
            
            if (!current.canTransitionTo(target)) {
                throw new RuntimeException("Invalid status transition from " + current + " to " + target);
            }
            
            signal.setStatus(target.name());
            return signalRepository.save(signal);
        });
    }

    @Override
    @Transactional
    public Signal voteForSignal(UUID signalId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found"));

        if (voteRepository.findByUserIdAndSignalId(user.getId(), signalId).isPresent()) {
            throw new RuntimeException("User already voted for this signal");
        }

        try {
            voteRepository.save(new Vote(user.getId(), signalId));
            signal.setCommunityVotes(signal.getCommunityVotes() + 1);
            signal.setPriorityScore(calculateScore(signal));
            return signalRepository.save(signal);
        } catch (DataIntegrityViolationException e) {
            // P1-12: Handling race conditions
            throw new RuntimeException("Double voting detected and rejected.");
        }
    }
}
