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
    
    // Duplicate detection
    Map<UUID, List<Signal>> findDuplicates();
    Signal mergeSignals(UUID targetId, List<UUID> duplicateIds);
    
    // Persistence
    Signal saveSignal(Signal signal);
}
