package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityProjectTaskResponse(
    UUID id,
    UUID projectBoardId,
    String title,
    String details,
    String status,
    UUID assigneeId,
    String assigneeUsername,
    LocalDate dueDate,
    int sortOrder,
    List<CommunityProjectTaskCommentResponse> comments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
