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
@Table(name = "community_moderation_reports")
public class CommunityModerationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityModerationTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private UUID reporterUserId;

    @Column(nullable = false)
    private UUID reportedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityModerationReasonCode reasonCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetContentPreview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityModerationReportStatus status = CommunityModerationReportStatus.OPEN;

    @Column(nullable = false)
    private boolean contentHidden = false;

    @Column(nullable = false)
    private boolean falsePositiveReviewRecommended = false;

    private UUID resolvedByUserId;

    @Column(columnDefinition = "TEXT")
    private String resolutionReason;

    private UUID linkedSanctionId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public CommunityModerationTargetType getTargetType() { return targetType; }
    public void setTargetType(CommunityModerationTargetType targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public UUID getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(UUID reporterUserId) { this.reporterUserId = reporterUserId; }
    public UUID getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(UUID reportedUserId) { this.reportedUserId = reportedUserId; }
    public CommunityModerationReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(CommunityModerationReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getTargetContentPreview() { return targetContentPreview; }
    public void setTargetContentPreview(String targetContentPreview) { this.targetContentPreview = targetContentPreview; }
    public CommunityModerationReportStatus getStatus() { return status; }
    public void setStatus(CommunityModerationReportStatus status) { this.status = status; }
    public boolean isContentHidden() { return contentHidden; }
    public void setContentHidden(boolean contentHidden) { this.contentHidden = contentHidden; }
    public boolean isFalsePositiveReviewRecommended() { return falsePositiveReviewRecommended; }
    public void setFalsePositiveReviewRecommended(boolean falsePositiveReviewRecommended) { this.falsePositiveReviewRecommended = falsePositiveReviewRecommended; }
    public UUID getResolvedByUserId() { return resolvedByUserId; }
    public void setResolvedByUserId(UUID resolvedByUserId) { this.resolvedByUserId = resolvedByUserId; }
    public String getResolutionReason() { return resolutionReason; }
    public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }
    public UUID getLinkedSanctionId() { return linkedSanctionId; }
    public void setLinkedSanctionId(UUID linkedSanctionId) { this.linkedSanctionId = linkedSanctionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}