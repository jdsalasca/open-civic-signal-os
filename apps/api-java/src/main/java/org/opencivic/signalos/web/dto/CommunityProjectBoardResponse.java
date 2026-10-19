package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityProjectBoardResponse(
    UUID id,
    UUID communityId,
    UUID linkedProposalId,
    String linkedProposalTitle,
    UUID ownerId,
    String ownerUsername,
    String title,
    String summary,
    LocalDate dueDate,
    CommunityProjectTaskCountsResponse taskCounts,
    List<CommunityProjectTaskResponse> tasks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
