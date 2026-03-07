package org.opencivic.signalos.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "community_proposals")
public class CommunityProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID authorId;

    private UUID relatedSignalId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 60)
    private String templateKey = "STANDARD_COMMUNITY_PROPOSAL";

    @Column(nullable = false, length = 40)
    private String status = "PROPOSED";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problemStatement;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String proposedSolution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String estimatedCost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String beneficiariesSummary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "community_proposal_links", joinColumns = @JoinColumn(name = "proposal_id"))
    @OrderColumn(name = "position_index")
    @Column(name = "url", nullable = false, length = 1200)
    private List<String> supportingLinks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityProposalVoteMode voteMode = CommunityProposalVoteMode.YES_NO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityProposalVoteVisibility voteVisibility = CommunityProposalVoteVisibility.COMMUNITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunityProposalVoteEligibility voteEligibility = CommunityProposalVoteEligibility.VERIFIED_MEMBERS;

    private LocalDateTime votingOpensAt;

    private LocalDateTime votingClosesAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public UUID getRelatedSignalId() { return relatedSignalId; }
    public void setRelatedSignalId(UUID relatedSignalId) { this.relatedSignalId = relatedSignalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getProposedSolution() { return proposedSolution; }
    public void setProposedSolution(String proposedSolution) { this.proposedSolution = proposedSolution; }
    public String getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(String estimatedCost) { this.estimatedCost = estimatedCost; }
    public String getBeneficiariesSummary() { return beneficiariesSummary; }
    public void setBeneficiariesSummary(String beneficiariesSummary) { this.beneficiariesSummary = beneficiariesSummary; }
    public List<String> getSupportingLinks() { return supportingLinks; }
    public void setSupportingLinks(List<String> supportingLinks) { this.supportingLinks = new ArrayList<>(supportingLinks); }
    public CommunityProposalVoteMode getVoteMode() { return voteMode; }
    public void setVoteMode(CommunityProposalVoteMode voteMode) { this.voteMode = voteMode; }
    public CommunityProposalVoteVisibility getVoteVisibility() { return voteVisibility; }
    public void setVoteVisibility(CommunityProposalVoteVisibility voteVisibility) { this.voteVisibility = voteVisibility; }
    public CommunityProposalVoteEligibility getVoteEligibility() { return voteEligibility; }
    public void setVoteEligibility(CommunityProposalVoteEligibility voteEligibility) { this.voteEligibility = voteEligibility; }
    public LocalDateTime getVotingOpensAt() { return votingOpensAt; }
    public void setVotingOpensAt(LocalDateTime votingOpensAt) { this.votingOpensAt = votingOpensAt; }
    public LocalDateTime getVotingClosesAt() { return votingClosesAt; }
    public void setVotingClosesAt(LocalDateTime votingClosesAt) { this.votingClosesAt = votingClosesAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
