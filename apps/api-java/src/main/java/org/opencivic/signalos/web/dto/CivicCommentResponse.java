package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CivicCommentResponse(
    UUID id,
    UUID parentId,
    String parentType,
    UUID authorId,
    String authorUsername,
    String authorRole,
    String content,
    LocalDateTime createdAt
) {}
