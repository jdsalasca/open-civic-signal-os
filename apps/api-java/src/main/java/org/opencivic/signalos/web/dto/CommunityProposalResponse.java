package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityProposalResponse(
    UUID id,
    UUID communityId,
    UUID authorId,
    String authorUsername,
    UUID relatedSignalId,
    String relatedSignalTitle,
    String title,
    String templateKey,
    String status,
    String problemStatement,
    String proposedSolution,
    String estimatedCost,
    String beneficiariesSummary,
    List<String> supportingLinks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
