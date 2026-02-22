package org.opencivic.signalos.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "community_thread_messages")
public class CommunityThreadMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID threadId;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private UUID sourceCommunityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private boolean hidden = false;

    @Column(columnDefinition = "TEXT")
    private String moderationReason;

    private UUID hiddenBy;
    private LocalDateTime hiddenAt;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "message_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "reaction_type")
    @Column(name = "count")
    private Map<String, Integer> reactions = new HashMap<>();

    public UUID getId() {
        return id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getSourceCommunityId() {
        return sourceCommunityId;
    }

    public void setSourceCommunityId(UUID sourceCommunityId) {
        this.sourceCommunityId = sourceCommunityId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public void setModerationReason(String moderationReason) {
        this.moderationReason = moderationReason;
    }

    public UUID getHiddenBy() {
        return hiddenBy;
    }

    public void setHiddenBy(UUID hiddenBy) {
        this.hiddenBy = hiddenBy;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Integer> getReactions() {
        if (reactions == null) reactions = new HashMap<>();
        return reactions;
    }

    public void setReactions(Map<String, Integer> reactions) {
        this.reactions = reactions;
    }
}
