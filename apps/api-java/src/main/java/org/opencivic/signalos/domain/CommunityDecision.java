package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_decisions")
public class CommunityDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    private UUID linkedProposalId;

    private UUID governanceDocumentId;

    private UUID projectBoardId;

    @Column(nullable = false)
    private UUID decidedBy;

    private UUID executionOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityDecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityDecisionStatus decisionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityDecisionBasisType approvalBasisType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String approvalBasisSummary;

    @Column(nullable = false)
    private LocalDateTime decidedAt = LocalDateTime.now();

    private LocalDate effectiveDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public UUID getLinkedProposalId() { return linkedProposalId; }
    public void setLinkedProposalId(UUID linkedProposalId) { this.linkedProposalId = linkedProposalId; }
    public UUID getGovernanceDocumentId() { return governanceDocumentId; }
    public void setGovernanceDocumentId(UUID governanceDocumentId) { this.governanceDocumentId = governanceDocumentId; }
    public UUID getProjectBoardId() { return projectBoardId; }
    public void setProjectBoardId(UUID projectBoardId) { this.projectBoardId = projectBoardId; }
    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID decidedBy) { this.decidedBy = decidedBy; }
    public UUID getExecutionOwnerId() { return executionOwnerId; }
    public void setExecutionOwnerId(UUID executionOwnerId) { this.executionOwnerId = executionOwnerId; }
    public CommunityDecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(CommunityDecisionType decisionType) { this.decisionType = decisionType; }
    public CommunityDecisionStatus getDecisionStatus() { return decisionStatus; }
    public void setDecisionStatus(CommunityDecisionStatus decisionStatus) { this.decisionStatus = decisionStatus; }
    public CommunityDecisionBasisType getApprovalBasisType() { return approvalBasisType; }
    public void setApprovalBasisType(CommunityDecisionBasisType approvalBasisType) { this.approvalBasisType = approvalBasisType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getApprovalBasisSummary() { return approvalBasisSummary; }
    public void setApprovalBasisSummary(String approvalBasisSummary) { this.approvalBasisSummary = approvalBasisSummary; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
