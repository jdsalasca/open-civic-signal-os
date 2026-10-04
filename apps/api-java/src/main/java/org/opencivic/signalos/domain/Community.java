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

    /**
     * A stable, public key for addressing this community from another instance.
     *
     * <p>Not the id, which is instance-local: a peer that had to learn a UUID minted by this
     * deployment before it could ask for anything could not bootstrap itself. Not the slug either,
     * which is mutable — a federation key that changes under a peer breaks it silently, with no error
     * on either side.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String federationKey;

    /**
     * Mints a federation key for any community saved without one.
     *
     * <p>A lifecycle callback rather than a line in the creation service, because the key is an
     * invariant of the entity: a community saved by a test, a seed or an admin path has to be
     * addressable too. Leaving it to one service would mean a null column that only shows up when a
     * peer tries to use it.
     */
    @jakarta.persistence.PrePersist
    void assignFederationKey() {
        if (federationKey == null || federationKey.isBlank()) {
            byte[] bytes = new byte[12];
            new java.security.SecureRandom().nextBytes(bytes);
            federationKey = java.util.HexFormat.of().formatHex(bytes);
        }
    }

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

    public String getFederationKey() {
        return federationKey;
    }

    public void setFederationKey(String federationKey) {
        this.federationKey = federationKey;
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
