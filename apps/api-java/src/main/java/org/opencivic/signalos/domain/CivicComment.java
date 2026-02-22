package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "civic_comments")
public class CivicComment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID parentId; // ID of Signal or Blog Post

    @Column(nullable = false)
    private String parentType; // "SIGNAL" or "BLOG"

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CivicComment() {}

    public CivicComment(UUID parentId, String parentType, UUID authorId, String content) {
        this.parentId = parentId;
        this.parentType = parentType;
        this.authorId = authorId;
        this.content = content;
    }

    public UUID getId() { return id; }
    public UUID getParentId() { return parentId; }
    public String getParentType() { return parentType; }
    public UUID getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public void setParentType(String parentType) { this.parentType = parentType; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public void setContent(String content) { this.content = content; }
}
