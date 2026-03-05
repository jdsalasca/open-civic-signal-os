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

    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String locationLabel;
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

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "community_id")
    private UUID communityId;
    
    private LocalDateTime createdAt;
    
    private Double latitude;
    private Double longitude;

    @ElementCollection
    @CollectionTable(name = "signal_evidence_urls", joinColumns = @JoinColumn(name = "signal_id"))
    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private List<String> evidenceUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "signal_reactions", joinColumns = @JoinColumn(name = "signal_id"))
    @MapKeyColumn(name = "reaction_type")
    @Column(name = "count")
    private java.util.Map<String, Integer> reactions = new java.util.HashMap<>();

    public java.util.Map<String, Integer> getReactions() {
        if (reactions == null) reactions = new java.util.HashMap<>();
        return reactions;
    }

    public void setReactions(java.util.Map<String, Integer> reactions) {
        this.reactions = reactions;
    }

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
    public String getImageUrl() { return imageUrl; }
    public String getLocationLabel() { return locationLabel; }
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
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public UUID getCommunityId() { return communityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public List<String> getEvidenceUrls() {
        if (evidenceUrls == null) evidenceUrls = new ArrayList<>();
        return evidenceUrls;
    }
    
    public List<UUID> getMergedFrom() { 
        if (mergedFrom == null) mergedFrom = new ArrayList<>();
        return mergedFrom; 
    }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }
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
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls != null ? new ArrayList<>(evidenceUrls) : new ArrayList<>();
    }

    public Signal withScore(double score, ScoreBreakdown breakdown) {
        this.priorityScore = score;
        this.scoreBreakdown = breakdown;
        return this;
    }
}
