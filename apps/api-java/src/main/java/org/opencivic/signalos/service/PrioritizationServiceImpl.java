package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrioritizationServiceImpl implements PrioritizationService {

    private final SignalRepository signalRepository;

    public PrioritizationServiceImpl(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    @Override
    public Page<Signal> getPrioritizedSignals(Pageable pageable) {
        // Now using DB-level pagination and sorting (if provided in pageable)
        return signalRepository.findAll(pageable)
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)));
    }

    @Override
    public List<Signal> getTopUnresolved(int limit) {
        return signalRepository.findAll().stream()
                .filter(signal -> "NEW".equals(signal.getStatus()))
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)))
                .sorted(Comparator.comparingDouble(Signal::getPriorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
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
                    processed.add(s2.id()); // Compatibility note: using getId() is better
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
    public Signal mergeSignals(UUID targetId, List<UUID> duplicateIds) {
        return signalRepository.findById(targetId).orElse(null);
    }

    @Override
    public Signal saveSignal(Signal signal) {
        return signalRepository.save(signal);
    }
}
