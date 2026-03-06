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
@Table(name = "community_proposal_deliberation_entries")
public class CommunityProposalDeliberationEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID proposalId;

    @Column(nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CommunityProposalDeliberationType entryType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 1200)
    private String supportingLink;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(length = 500)
    private String moderationReason;

    private UUID hiddenBy;

    private LocalDateTime hiddenAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public void setProposalId(UUID proposalId) { this.proposalId = proposalId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public CommunityProposalDeliberationType getEntryType() { return entryType; }
    public void setEntryType(CommunityProposalDeliberationType entryType) { this.entryType = entryType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSupportingLink() { return supportingLink; }
    public void setSupportingLink(String supportingLink) { this.supportingLink = supportingLink; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public UUID getHiddenBy() { return hiddenBy; }
    public void setHiddenBy(UUID hiddenBy) { this.hiddenBy = hiddenBy; }
    public LocalDateTime getHiddenAt() { return hiddenAt; }
    public void setHiddenAt(LocalDateTime hiddenAt) { this.hiddenAt = hiddenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
