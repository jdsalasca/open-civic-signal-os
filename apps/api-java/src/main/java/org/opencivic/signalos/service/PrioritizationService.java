package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PrioritizationService {
    List<Signal> getPrioritizedSignals();
    List<Signal> getTopUnresolved(int limit);
    double calculateScore(Signal signal);
    ScoreBreakdown getBreakdown(Signal signal);
    
    // Duplicate detection
    Map<UUID, List<Signal>> findDuplicates();
    Signal mergeSignals(UUID targetId, List<UUID> duplicateIds);
}
