package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrioritizationServiceImpl implements PrioritizationService {

    private final SignalRepository signalRepository;

    public PrioritizationServiceImpl(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    @Override
    public List<Signal> getPrioritizedSignals() {
        return signalRepository.findAll().stream()
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)))
                .sorted(Comparator.comparingDouble(Signal::priorityScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Signal> getTopUnresolved(int limit) {
        return signalRepository.findAll().stream()
                .filter(signal -> "NEW".equals(signal.status()))
                .map(signal -> signal.withScore(calculateScore(signal), getBreakdown(signal)))
                .sorted(Comparator.comparingDouble(Signal::priorityScore).reversed())
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
            signal.urgency() * 30.0,
            signal.impact() * 25.0,
            Math.min(signal.affectedPeople() / 10.0, 30.0),
            Math.min(signal.communityVotes() / 5.0, 15.0)
        );
    }
}
