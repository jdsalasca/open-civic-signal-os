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
@Table(name = "community_open_data_access_logs")
public class CommunityOpenDataAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    private UUID actorUserId;

    private UUID tokenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityOpenDataAccessChannel accessChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityOpenDataExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityOpenDataFormat format;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public CommunityOpenDataAccessChannel getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(CommunityOpenDataAccessChannel accessChannel) {
        this.accessChannel = accessChannel;
    }

    public CommunityOpenDataExportType getExportType() {
        return exportType;
    }

    public void setExportType(CommunityOpenDataExportType exportType) {
        this.exportType = exportType;
    }

    public CommunityOpenDataFormat getFormat() {
        return format;
    }

    public void setFormat(CommunityOpenDataFormat format) {
        this.format = format;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
