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

    // Getters and Setters
    public UUID id() { return id; }
    public String title() { return title; }
    public String status() { return status; }
    public double priorityScore() { return priorityScore; }
    public int urgency() { return urgency; }
    public int impact() { return impact; }
    public int affectedPeople() { return affectedPeople; }
    public int communityVotes() { return communityVotes; }
    public String category() { return category; }
    public ScoreBreakdown scoreBreakdown() { return scoreBreakdown; }

    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public Signal withScore(double score, ScoreBreakdown breakdown) {
        this.priorityScore = score;
        this.scoreBreakdown = breakdown;
        return this;
    }
}
