package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "community_open_data_tokens")
public class CommunityOpenDataToken implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 255)
    private String tokenHash;

    @Column(nullable = false, length = 36)
    private String tokenPrefix;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String scopesCsv;

    @Column(nullable = false)
    private int rateLimitPerHour = 120;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastUsedAt;

    private LocalDateTime revokedAt;

    @Transient
    private boolean isNew = true;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getScopesCsv() {
        return scopesCsv;
    }

    public void setScopesCsv(String scopesCsv) {
        this.scopesCsv = scopesCsv;
    }

    public int getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public void setRateLimitPerHour(int rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public List<CommunityOpenDataTokenScope> getScopes() {
        if (scopesCsv == null || scopesCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(scopesCsv.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(CommunityOpenDataTokenScope::valueOf)
            .toList();
    }

    public void setScopes(List<CommunityOpenDataTokenScope> scopes) {
        this.scopesCsv = scopes == null
            ? ""
            : scopes.stream().distinct().sorted(java.util.Comparator.comparing(Enum::name)).map(Enum::name).collect(Collectors.joining(","));
    }
}
