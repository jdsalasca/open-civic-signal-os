package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A recorded proposal to change how signals are scored, with the effect it would have had.
 *
 * <p>Important: nothing reads this entity when scoring. Applying a proposal means changing
 * {@link PrioritizationFormula}, bumping the formula version, and writing an ADR. Storing a row
 * with a different status must never be able to change what a resident sees, which is why there is
 * no column the scoring code consults.
 */
@Entity
@Table(name = "formula_change_proposals")
public class FormulaChangeProposal {

    public enum Status {
        PROPOSED,
        APPROVED,
        REJECTED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String proposedWeights;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String currentWeights;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String impactPreview;

    @Column(nullable = false)
    private int sampleSize;

    @Column(nullable = false)
    private int positionsMoved;

    @Column(nullable = false)
    private UUID proposedBy;

    @Column(nullable = false)
    private LocalDateTime proposedAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String status = Status.PROPOSED.name();

    private UUID decidedBy;

    private LocalDateTime decidedAt;

    @Column(columnDefinition = "TEXT")
    private String decisionNote;

    public FormulaChangeProposal() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getProposedWeights() {
        return proposedWeights;
    }

    public void setProposedWeights(String proposedWeights) {
        this.proposedWeights = proposedWeights;
    }

    public String getCurrentWeights() {
        return currentWeights;
    }

    public void setCurrentWeights(String currentWeights) {
        this.currentWeights = currentWeights;
    }

    public String getImpactPreview() {
        return impactPreview;
    }

    public void setImpactPreview(String impactPreview) {
        this.impactPreview = impactPreview;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public int getPositionsMoved() {
        return positionsMoved;
    }

    public void setPositionsMoved(int positionsMoved) {
        this.positionsMoved = positionsMoved;
    }

    public UUID getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(UUID proposedBy) {
        this.proposedBy = proposedBy;
    }

    public LocalDateTime getProposedAt() {
        return proposedAt;
    }

    public void setProposedAt(LocalDateTime proposedAt) {
        this.proposedAt = proposedAt;
    }

    public Status getStatus() {
        return Status.valueOf(status);
    }

    public void setStatus(Status status) {
        this.status = status.name();
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

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}