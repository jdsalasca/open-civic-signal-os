package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import java.util.List;

public interface PrioritizationService {
    List<Signal> getPrioritizedSignals();
    List<Signal> getTopUnresolved(int limit);
    double calculateScore(Signal signal);
    ScoreBreakdown getBreakdown(Signal signal);
}
