package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_membership_audit")
public class CommunityMembershipAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID targetUserId;

    @Column(nullable = false)
    private UUID changedBy;

    @Enumerated(EnumType.STRING)
    private CommunityRole previousRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityRole newRole;

    private LocalDateTime changedAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public CommunityRole getPreviousRole() {
        return previousRole;
    }

    public void setPreviousRole(CommunityRole previousRole) {
        this.previousRole = previousRole;
    }

    public CommunityRole getNewRole() {
        return newRole;
    }

    public void setNewRole(CommunityRole newRole) {
        this.newRole = newRole;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
