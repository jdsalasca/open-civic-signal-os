package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One resident's answer to the trust pulse for one closed month.
 *
 * <p>Kept separate from the computed trust metrics on purpose. Those measure what the platform did;
 * this measures what residents think of it. Rolling them into one number would let the platform
 * grade its own homework, which is the opposite of what a trust metric is for.
 */
@Entity
@Table(name = "community_trust_pulse_responses")
public class CommunityTrustPulseResponse {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID respondentId;

    /** The closed month this judgement is about, as YYYY-MM. */
    @Column(nullable = false, length = 7)
    private String periodKey;

    @Column(nullable = false)
    private int trustScore;

    @Column(nullable = false)
    private int responsivenessScore;

    @Column(nullable = false)
    private int transparencyScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    public CommunityTrustPulseResponse() {}

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

    public UUID getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(UUID respondentId) {
        this.respondentId = respondentId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = trustScore;
    }

    public int getResponsivenessScore() {
        return responsivenessScore;
    }

    public void setResponsivenessScore(int responsivenessScore) {
        this.responsivenessScore = responsivenessScore;
    }

    public int getTransparencyScore() {
        return transparencyScore;
    }

    public void setTransparencyScore(int transparencyScore) {
        this.transparencyScore = transparencyScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}