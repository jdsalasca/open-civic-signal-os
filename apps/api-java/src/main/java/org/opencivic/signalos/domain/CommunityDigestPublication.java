package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A weekly digest that was actually published.
 *
 * <p>The unique index on (community, week) is the point of this entity. An email digest delivered
 * twice is not a cosmetic bug: residents receive the same bulletin again, and the platform looks
 * broken at exactly the moment it is asking to be trusted.
 *
 * <p>{@code contentHash} covers the rendered body, so a recipient can verify the digest they
 * received is the one that was recorded rather than a later edit.
 */
@Entity
@Table(name = "community_digest_publications")
public class CommunityDigestPublication {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    /** ISO week, for example 2026-W13. */
    @Column(nullable = false, length = 10)
    private String weekKey;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false)
    private int unresolvableItemCount;

    @Column(nullable = false)
    private UUID publishedBy;

    @Column(nullable = false)
    private LocalDateTime publishedAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    public CommunityDigestPublication() {}

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

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getUnresolvableItemCount() {
        return unresolvableItemCount;
    }

    public void setUnresolvableItemCount(int unresolvableItemCount) {
        this.unresolvableItemCount = unresolvableItemCount;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}