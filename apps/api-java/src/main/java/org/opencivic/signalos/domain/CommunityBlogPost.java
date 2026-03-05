package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_blog_posts")
public class CommunityBlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String statusTag;

    private LocalDateTime publishedAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "blog_reactions", joinColumns = @jakarta.persistence.JoinColumn(name = "blog_id"))
    @jakarta.persistence.MapKeyColumn(name = "reaction_type")
    @jakarta.persistence.Column(name = "count")
    private java.util.Map<String, Integer> reactions = new java.util.HashMap<>();

    public java.util.Map<String, Integer> getReactions() {
        if (reactions == null) reactions = new java.util.HashMap<>();
        return reactions;
    }

    public void setReactions(java.util.Map<String, Integer> reactions) {
        this.reactions = reactions;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatusTag() {
        return statusTag;
    }

    public void setStatusTag(String statusTag) {
        this.statusTag = statusTag;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
