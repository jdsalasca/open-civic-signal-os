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
    
    @Transient
    private ScoreBreakdown scoreBreakdown;
    
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String moderationReason;
    
    // P1-8: Inicialización defensiva para evitar NullPointerException
    @ElementCollection
    private List<UUID> mergedFrom = new ArrayList<>();
    
    private LocalDateTime createdAt;

    public Signal() {}

    public Signal(UUID id, String title, String description, String category, int urgency, int impact, int affectedPeople, int communityVotes, double priorityScore, ScoreBreakdown scoreBreakdown, String status, List<UUID> mergedFrom, LocalDateTime createdAt) {
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
        this.createdAt = createdAt;
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
    public List<UUID> getMergedFrom() { 
        if (mergedFrom == null) mergedFrom = new ArrayList<>();
        return mergedFrom; 
    }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
    public void setCommunityVotes(int communityVotes) { this.communityVotes = communityVotes; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }

    public Signal withScore(double score, ScoreBreakdown breakdown) {
        this.priorityScore = score;
        this.scoreBreakdown = breakdown;
        return this;
    }
}
