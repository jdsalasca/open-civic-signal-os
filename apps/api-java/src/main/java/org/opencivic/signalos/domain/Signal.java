package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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
    private ScoreBreakdown scoreBreakdown;
    
    private String status;
    
    @ElementCollection
    private List<UUID> mergedFrom;
    
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
        this.mergedFrom = mergedFrom;
        this.createdAt = createdAt;
    }

    // Getters and Setters (Standard naming)
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public double getPriorityScore() { return priorityScore; }
    public int getUrgency() { return urgency; }
    public int getImpact() { return impact; }
    public int getAffectedPeople() { return affectedPeople; }
    public int getCommunityVotes() { return communityVotes; }
    public String getCategory() { return category; }
    public ScoreBreakdown getScoreBreakdown() { return scoreBreakdown; }

    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public Signal withScore(double score, ScoreBreakdown breakdown) {
        this.priorityScore = score;
        this.scoreBreakdown = breakdown;
        return this;
    }
}
