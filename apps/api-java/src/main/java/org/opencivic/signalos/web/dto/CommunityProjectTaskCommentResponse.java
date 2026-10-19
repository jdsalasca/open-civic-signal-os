package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityProjectTaskCommentResponse(
    UUID id,
    UUID taskId,
    UUID authorId,
    String authorUsername,
    String content,
    LocalDateTime createdAt
) {}
