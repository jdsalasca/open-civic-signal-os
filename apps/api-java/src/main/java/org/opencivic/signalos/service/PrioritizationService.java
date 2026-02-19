package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PrioritizationService {
    Page<Signal> getPrioritizedSignals(Pageable pageable);
    List<Signal> getTopUnresolved(int limit);
    Optional<Signal> getSignalById(UUID id);
    double calculateScore(Signal signal);
    ScoreBreakdown getBreakdown(Signal signal);
    Map<UUID, List<Signal>> findDuplicates();
    Signal mergeSignals(UUID targetId, List<UUID> duplicateIds);
    
    // P1-B: Paginated access to moderation queue
    Page<Signal> getFlaggedSignals(Pageable pageable);
    
    Signal moderateSignal(UUID id, String action, String reason);
    Signal saveSignal(Signal signal);
    Optional<Signal> updateStatus(UUID id, String newStatus);
    Signal voteForSignal(UUID signalId, String username);
}
