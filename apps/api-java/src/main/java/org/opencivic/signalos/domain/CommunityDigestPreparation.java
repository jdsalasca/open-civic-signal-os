package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The digest the scheduler prepared, kept exactly as it was composed.
 *
 * <p>This exists because "prepared" was a promise the code did not keep. The scheduler composed a
 * digest, read its item count, and discarded the rest; publishing recomposed from live data. A
 * coordinator could review one digest on Monday and publish a different one on Wednesday, and the
 * content hash a resident received would correspond to nothing anybody had looked at.
 *
 * <p>So the prepared artifact is the artifact. Recomposition happens only when no preparation exists
 * for the week, which is the case where a coordinator runs the digest by hand with no scheduler
 * involved.
 *
 * <p>The counts are stored rather than recomputed because they appear in the rendered body, and the
 * body is what residents receive. Anything not stored here cannot be reproduced from a review.
 */
@Entity
@Table(name = "community_digest_preparations")
public class CommunityDigestPreparation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 10)
    private String weekKey;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    @Column(nullable = false, length = 10)
    private String previousWeekKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 64)
    private String contentHash;

    /** The rendered items, as JSON, so a preview shows the same rows that will be published. */
    @Column(columnDefinition = "TEXT")
    private String itemsJson;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false)
    private int resolvedThisWeek;

    @Column(nullable = false)
    private int rejectedThisWeek;

    @Column(nullable = false)
    private int reportedThisWeek;

    @Column(nullable = false)
    private int stillOpenTotal;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    public CommunityDigestPreparation() {}

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

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public String getPreviousWeekKey() {
        return previousWeekKey;
    }

    public void setPreviousWeekKey(String previousWeekKey) {
        this.previousWeekKey = previousWeekKey;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getResolvedThisWeek() {
        return resolvedThisWeek;
    }

    public void setResolvedThisWeek(int resolvedThisWeek) {
        this.resolvedThisWeek = resolvedThisWeek;
    }

    public int getRejectedThisWeek() {
        return rejectedThisWeek;
    }

    public void setRejectedThisWeek(int rejectedThisWeek) {
        this.rejectedThisWeek = rejectedThisWeek;
    }

    public int getReportedThisWeek() {
        return reportedThisWeek;
    }

    public void setReportedThisWeek(int reportedThisWeek) {
        this.reportedThisWeek = reportedThisWeek;
    }

    public int getStillOpenTotal() {
        return stillOpenTotal;
    }

    public void setStillOpenTotal(int stillOpenTotal) {
        this.stillOpenTotal = stillOpenTotal;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}