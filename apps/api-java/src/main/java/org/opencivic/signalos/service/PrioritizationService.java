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

    /**
     * Adds a minimum-score filter pushed into the query.
     *
     * <p>Only the two-argument-plus-statuses variant exists. Optional arguments that only
     * influence a null check in the body are one more combination to keep straight and to test,
     * for no caller benefit.
     */
    Page<Signal> getPrioritizedSignals(
        Pageable pageable,
        UUID communityId,
        Collection<String> statuses,
        Double minScore
    );
    List<Signal> getTopUnresolved(int limit);
    List<Signal> getTopUnresolved(int limit, UUID communityId);

    /** Same threshold contract as the paginated ranking, so both agree on what "critical" means. */
    List<Signal> getTopUnresolved(int limit, UUID communityId, Double minScore);
    Optional<Signal> getSignalById(UUID id);
    Optional<Signal> getSignalById(UUID id, UUID communityId);
    double calculateScore(Signal signal);
    ScoreBreakdown getBreakdown(Signal signal);
    TrustPacket getTrustPacket(UUID signalId);
    java.util.List<org.opencivic.signalos.domain.SignalStatusEntry> getStatusHistory(UUID signalId);
    Signal assignSignal(UUID signalId, String assigneeUsername, String changedBy, String reason);
    java.util.Map<UUID, java.util.List<Signal>> findDuplicates();
    java.util.Map<UUID, java.util.List<Signal>> findDuplicates(UUID communityId);
    Signal mergeSignals(UUID targetId, java.util.List<UUID> duplicateIds);
    
    // P1-B: Paginated access to moderation queue
    Page<Signal> getFlaggedSignals(Pageable pageable);
    
    Signal moderateSignal(UUID id, String action, String reason);
Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String username);
    Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String username, UUID communityId);
    Signal createSignal(String title, String description, String category, int urgency, int impact, int affectedPeople, String imageUrl, String locationLabel, List<String> evidenceUrls, Double latitude, Double longitude, String sourceChannel, String sourceRef, String username, UUID communityId);
    Signal saveSignal(Signal signal);
    Optional<Signal> updateStatus(UUID id, String newStatus);
    Optional<Signal> updateStatus(UUID id, String newStatus, String changedBy, String reason);
    Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId);
    Optional<Signal> updateStatus(UUID id, String newStatus, UUID communityId, String changedBy, String reason);
    Signal voteForSignal(UUID signalId, String username);
    Signal voteForSignal(UUID signalId, String username, UUID communityId);
}
