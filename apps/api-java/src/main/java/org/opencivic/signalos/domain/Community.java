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
@Table(name = "communities")
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private UUID parentCommunityId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private CommunityOpenDataPolicy openDataPolicy = CommunityOpenDataPolicy.DISABLED;

    private UUID privacyUpdatedBy;

    private LocalDateTime privacyUpdatedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getParentCommunityId() {
        return parentCommunityId;
    }

    public void setParentCommunityId(UUID parentCommunityId) {
        this.parentCommunityId = parentCommunityId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CommunityOpenDataPolicy getOpenDataPolicy() {
        return openDataPolicy;
    }

    public void setOpenDataPolicy(CommunityOpenDataPolicy openDataPolicy) {
        this.openDataPolicy = openDataPolicy;
    }

    public UUID getPrivacyUpdatedBy() {
        return privacyUpdatedBy;
    }

    public void setPrivacyUpdatedBy(UUID privacyUpdatedBy) {
        this.privacyUpdatedBy = privacyUpdatedBy;
    }

    public LocalDateTime getPrivacyUpdatedAt() {
        return privacyUpdatedAt;
    }

    public void setPrivacyUpdatedAt(LocalDateTime privacyUpdatedAt) {
        this.privacyUpdatedAt = privacyUpdatedAt;
    }
}
