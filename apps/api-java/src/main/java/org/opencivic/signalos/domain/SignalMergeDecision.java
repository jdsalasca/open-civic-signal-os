package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A human decision about an algorithm-suggested merge.
 *
 * <p>This project does not let an algorithm silently combine what residents wrote separately.
 * Duplicates are suggested; a person decides; the decision and the suggestion it was based on are
 * both recorded.
 *
 * <p>{@code suggestedSimilarities} is kept verbatim rather than recomputed. A reviewer deciding
 * today needs to see what the suggestion said <em>at the time</em>, and a re-run next month with a
 * tweaked threshold would otherwise rewrite the history of what people were shown.
 */
@Entity
@Table(name = "signal_merge_decisions")
public class SignalMergeDecision {

    public enum Decision {
        APPROVED,
        REJECTED,
        SPLIT
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID targetSignalId;

    @Column(nullable = false)
    private UUID decidedBy;

    @Column(nullable = false)
    private LocalDateTime decidedAt = LocalDateTime.now();

    @Column(nullable = false)
    private double similarityThreshold;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestedSimilarities;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mergedSignalIds;

    @Column(nullable = false, length = 20)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String note;

    public SignalMergeDecision() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getTargetSignalId() {
        return targetSignalId;
    }

    public void setTargetSignalId(UUID targetSignalId) {
        this.targetSignalId = targetSignalId;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public String getSuggestedSimilarities() {
        return suggestedSimilarities;
    }

    public void setSuggestedSimilarities(String suggestedSimilarities) {
        this.suggestedSimilarities = suggestedSimilarities;
    }

    public String getMergedSignalIds() {
        return mergedSignalIds;
    }

    public void setMergedSignalIds(String mergedSignalIds) {
        this.mergedSignalIds = mergedSignalIds;
    }

    public Decision getDecision() {
        return Decision.valueOf(decision);
    }

    public void setDecision(Decision decision) {
        this.decision = decision.name();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}