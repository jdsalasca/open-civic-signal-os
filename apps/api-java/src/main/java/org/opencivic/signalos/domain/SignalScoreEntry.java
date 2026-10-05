package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One recorded score for one signal, with the components that produced it.
 *
 * <p>{@code priorityScore} is mutable and had no history, so a transparency report about a closed month
 * showed the score the item carries now. The components travel with the score so the history explains
 * itself: a reader six months from now can see <em>why</em> a score was what it was, which is the same
 * standard every ranking claim in this platform is held to.
 *
 * <p>{@link #cause} records why the score moved. A score that changed because a resident supported the
 * issue is a different fact from one that changed because two duplicates were merged, and an audit
 * trail that cannot tell them apart is not much of an audit trail.
 */
@Entity
@Table(name = "signal_score_entries")
public class SignalScoreEntry {

    public enum Cause {
        /** The signal was recorded and scored for the first time. */
        INGEST,
        /** A resident supported the issue, raising its community votes. */
        SUPPORT_VOTE,
        /** Duplicates were merged into this signal, summing their votes. */
        DUPLICATE_MERGE
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID signalId;

    @Column(nullable = false)
    private double priorityScore;

    @Column(nullable = false)
    private int urgency;

    @Column(nullable = false)
    private int impact;

    @Column(nullable = false)
    private int affectedPeople;

    @Column(nullable = false)
    private int communityVotes;

    @Column(nullable = false, length = 20)
    private String formulaVersion;

    @Column(nullable = false, length = 30)
    private String cause;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public SignalScoreEntry() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSignalId() {
        return signalId;
    }

    public void setSignalId(UUID signalId) {
        this.signalId = signalId;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public int getUrgency() {
        return urgency;
    }

    public void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    public int getImpact() {
        return impact;
    }

    public void setImpact(int impact) {
        this.impact = impact;
    }

    public int getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(int affectedPeople) {
        this.affectedPeople = affectedPeople;
    }

    public int getCommunityVotes() {
        return communityVotes;
    }

    public void setCommunityVotes(int communityVotes) {
        this.communityVotes = communityVotes;
    }

    public String getFormulaVersion() {
        return formulaVersion;
    }

    public void setFormulaVersion(String formulaVersion) {
        this.formulaVersion = formulaVersion;
    }

    public Cause getCause() {
        return Cause.valueOf(cause);
    }

    public void setCause(Cause cause) {
        this.cause = cause.name();
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}