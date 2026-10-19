package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What the weekly digest scheduler did for one community in one week.
 *
 * <p>There is deliberately no {@code PUBLISHED} outcome. The scheduler prepares a digest and records
 * that it is ready; publishing remains a deliberate act by a person. A scheduler that sent bulletins
 * on its own would mean a wrong digest reaches residents with nobody accountable for it, which is the
 * question this design exists to answer rather than defer.
 */
@Entity
@Table(name = "digest_schedule_runs")
public class DigestScheduleRun {

    public enum Outcome {
        /** The digest was generated and is ready for a person to publish. */
        PREPARED,
        /** There was nothing to do: already published, or no channel configured. */
        SKIPPED,
        /** The digest could not be generated. */
        FAILED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 10)
    private String weekKey;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false)
    private LocalDateTime ranAt = LocalDateTime.now();

    public DigestScheduleRun() {}

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

    public String getWeekKey() {
        return weekKey;
    }

    public void setWeekKey(String weekKey) {
        this.weekKey = weekKey;
    }

    public Outcome getOutcome() {
        return Outcome.valueOf(outcome);
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome.name();
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getRanAt() {
        return ranAt;
    }

    public void setRanAt(LocalDateTime ranAt) {
        this.ranAt = ranAt;
    }
}