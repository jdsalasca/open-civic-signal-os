package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_threads")
public class CommunityThread {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sourceCommunityId;

    @Column(nullable = false)
    private UUID targetCommunityId;

    private UUID relatedSignalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private UUID createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public UUID getSourceCommunityId() {
        return sourceCommunityId;
    }

    public void setSourceCommunityId(UUID sourceCommunityId) {
        this.sourceCommunityId = sourceCommunityId;
    }

    public UUID getTargetCommunityId() {
        return targetCommunityId;
    }

    public void setTargetCommunityId(UUID targetCommunityId) {
        this.targetCommunityId = targetCommunityId;
    }

    public UUID getRelatedSignalId() {
        return relatedSignalId;
    }

    public void setRelatedSignalId(UUID relatedSignalId) {
        this.relatedSignalId = relatedSignalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
