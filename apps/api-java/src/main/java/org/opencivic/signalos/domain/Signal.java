package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "signals")
public class Signal {
    @Id
    private UUID id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String category;
    private int urgency;
    private int impact;
    private int affectedPeople;
    private int communityVotes;
    private double priorityScore;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "urgency", column = @Column(name = "score_urgency")),
        @AttributeOverride(name = "impact", column = @Column(name = "score_impact")),
        @AttributeOverride(name = "affectedPeople", column = @Column(name = "score_affected_people")),
        @AttributeOverride(name = "communityVotes", column = @Column(name = "score_community_votes"))
    })
    private ScoreBreakdown scoreBreakdown;
    
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String moderationReason;
    
    @ElementCollection
    private List<UUID> mergedFrom = new ArrayList<>();
    
    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "community_id")
    private UUID communityId;
    
    private LocalDateTime createdAt;

    public Signal() {}

    public Signal(UUID id, String title, String description, String category, int urgency, int impact, int affectedPeople, int communityVotes, double priorityScore, ScoreBreakdown scoreBreakdown, String status, List<UUID> mergedFrom, UUID authorId, LocalDateTime createdAt) {
        this(id, title, description, category, urgency, impact, affectedPeople, communityVotes, priorityScore, scoreBreakdown, status, mergedFrom, authorId, createdAt, null);
    }

    public Signal(UUID id, String title, String description, String category, int urgency, int impact, int affectedPeople, int communityVotes, double priorityScore, ScoreBreakdown scoreBreakdown, String status, List<UUID> mergedFrom, UUID authorId, LocalDateTime createdAt, UUID communityId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.urgency = urgency;
        this.impact = impact;
        this.affectedPeople = affectedPeople;
        this.communityVotes = communityVotes;
        this.priorityScore = priorityScore;
        this.scoreBreakdown = scoreBreakdown;
        this.status = status;
        this.mergedFrom = mergedFrom != null ? mergedFrom : new ArrayList<>();
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.communityId = communityId;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public double getPriorityScore() { return priorityScore; }
    public int getUrgency() { return urgency; }
    public int getImpact() { return impact; }
    public int getAffectedPeople() { return affectedPeople; }
    public int getCommunityVotes() { return communityVotes; }
    public String getCategory() { return category; }
    public ScoreBreakdown getScoreBreakdown() { return scoreBreakdown; }
    public String getModerationReason() { return moderationReason; }
    public UUID getAuthorId() { return authorId; }
    public UUID getCommunityId() { return communityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public List<UUID> getMergedFrom() { 
        if (mergedFrom == null) mergedFrom = new ArrayList<>();
        return mergedFrom; 
    }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setUrgency(int urgency) { this.urgency = urgency; }
    public void setImpact(int impact) { this.impact = impact; }
    public void setAffectedPeople(int affectedPeople) { this.affectedPeople = affectedPeople; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setStatus(String status) { this.status = status; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
    public void setCommunityVotes(int communityVotes) { this.communityVotes = communityVotes; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public Signal withScore(double score, ScoreBreakdown breakdown) {
        this.priorityScore = score;
        this.scoreBreakdown = breakdown;
        return this;
    }
}
