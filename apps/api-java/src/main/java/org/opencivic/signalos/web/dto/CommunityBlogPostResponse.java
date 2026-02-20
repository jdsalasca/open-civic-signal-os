package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityBlogPostResponse(
    UUID id,
    UUID communityId,
    UUID authorId,
    String authorUsername,
    String authorRole,
    String title,
    String content,
    String statusTag,
    LocalDateTime publishedAt,
    LocalDateTime updatedAt
) {}
