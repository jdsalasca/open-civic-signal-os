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
@Table(name = "community_proposal_votes")
public class CommunityProposalVote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID proposalId;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID voterId;

    @Column(nullable = false, length = 120)
    private String voterUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityRole membershipRole;

    @Column(nullable = false)
    private boolean verifiedMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityProposalVoteMode voteMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CommunityProposalVoteChoice choice;

    private Integer scoreValue;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public void setProposalId(UUID proposalId) { this.proposalId = proposalId; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public UUID getVoterId() { return voterId; }
    public void setVoterId(UUID voterId) { this.voterId = voterId; }
    public String getVoterUsername() { return voterUsername; }
    public void setVoterUsername(String voterUsername) { this.voterUsername = voterUsername; }
    public CommunityRole getMembershipRole() { return membershipRole; }
    public void setMembershipRole(CommunityRole membershipRole) { this.membershipRole = membershipRole; }
    public boolean isVerifiedMember() { return verifiedMember; }
    public void setVerifiedMember(boolean verifiedMember) { this.verifiedMember = verifiedMember; }
    public CommunityProposalVoteMode getVoteMode() { return voteMode; }
    public void setVoteMode(CommunityProposalVoteMode voteMode) { this.voteMode = voteMode; }
    public CommunityProposalVoteChoice getChoice() { return choice; }
    public void setChoice(CommunityProposalVoteChoice choice) { this.choice = choice; }
    public Integer getScoreValue() { return scoreValue; }
    public void setScoreValue(Integer scoreValue) { this.scoreValue = scoreValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
