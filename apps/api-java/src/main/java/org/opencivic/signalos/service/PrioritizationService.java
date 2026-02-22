package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.opencivic.signalos.web.dto.TrustPacket;

public interface PrioritizationService {
    Page<Signal> getPrioritizedSignals(Pageable pageable);
    Page<Signal> getPrioritizedSignals(Pageable pageable, UUID communityId);
    Page<Signal> getPrioritizedSignals(Pageable pageable, UUID communityId, Collection<String> statuses);
    List<Signal> getTopUnresolved(int limit);
    List<Signal> getTopUnresolved(int limit, UUID communityId);
    Optional<Signal> getSignalById(UUID id);
    Optional<Signal> getSignalById(UUID id, UUID communityId);
    double calculateScore(Signal signal);
    ScoreBreakdown getBreakdown(Signal signal);
    TrustPacket getTrustPacket(UUID signalId);
    java.util.List<org.opencivic.signalos.domain.SignalStatusEntry> getStatusHistory(UUID signalId);
    java.util.Map<UUID, java.util.List<Signal>> findDuplicates();
    java.util.Map<UUID, java.util.List<Signal>> findDuplicates(UUID communityId);
    Signal mergeSignals(UUID targetId, java.util.List<UUID> duplicateIds);
    
    // P1-B: Paginated access to moderation queue
    Page<Signal> getFlaggedSignals(Pageable pageable);
    
    Signal moderateSignal(UUID id, String action, String reason);
    Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String username);
    Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String username, UUID communityId);
    Signal saveSignal(Signal signal);
    Optional<Signal> updateStatus(UUID id, String newStatus);
    Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId);
    Signal voteForSignal(UUID signalId, String username);
    Signal voteForSignal(UUID signalId, String username, UUID communityId);
}
