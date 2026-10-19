package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A backlog ranking that was published, in a way a visitor can later check.
 *
 * <p>The explainability snapshot already freezes what an assembly saw. This is a different claim:
 * it names <em>which</em> frozen ranking the public backlog is currently serving, and records an
 * ordering hash so anyone can ask "is what I am looking at now the thing that was published?".
 *
 * <p>Without it, a ranking could change under visitors with nothing to compare against. The
 * ordering hash is over id and score only, deliberately: correcting a title should not make a
 * published backlog look tampered with.
 */
@Entity
@Table(name = "community_backlog_publications")
public class CommunityBacklogPublication {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID snapshotId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, length = 64)
    private String orderingHash;

    @Column(nullable = false)
    private String formulaVersion;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false)
    private UUID publishedBy;

    @Column(nullable = false)
    private LocalDateTime publishedAt = LocalDateTime.now();

    /**
     * 1 when this publication is the live one, null when superseded.
     *
     * <p>A nullable slot rather than a boolean, because the guarantee wanted is "at most one current
     * per community" and the portable way to express that is a unique index over
     * {@code (community_id, current_slot)} where nulls never collide. A partial unique index would
     * read better and does not exist on H2, which the test suite runs on.
     */
    @Column(name = "current_slot")
    private Integer currentSlot = 1;

    public CommunityBacklogPublication() {}

    public boolean isCurrent() {
        return currentSlot != null;
    }

    public void setCurrent(boolean current) {
        this.currentSlot = current ? 1 : null;
    }

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

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOrderingHash() {
        return orderingHash;
    }

    public void setOrderingHash(String orderingHash) {
        this.orderingHash = orderingHash;
    }

    public String getFormulaVersion() {
        return formulaVersion;
    }

    public void setFormulaVersion(String formulaVersion) {
        this.formulaVersion = formulaVersion;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UUID publishedBy) {
        this.publishedBy = publishedBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getCurrentSlot() {
        return currentSlot;
    }

    public void setCurrentSlot(Integer currentSlot) {
        this.currentSlot = currentSlot;
    }
}