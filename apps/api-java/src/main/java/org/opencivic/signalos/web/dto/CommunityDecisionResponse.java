package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityDecisionResponse(
    UUID id,
    UUID communityId,
    UUID linkedProposalId,
    String linkedProposalTitle,
    UUID governanceDocumentId,
    String governanceDocumentTitle,
    UUID projectBoardId,
    String projectBoardTitle,
    UUID decidedBy,
    String decidedByUsername,
    UUID executionOwnerId,
    String executionOwnerUsername,
    String decisionType,
    String decisionStatus,
    String approvalBasisType,
    String approvalBasisSummary,
    String title,
    String summary,
    LocalDateTime decidedAt,
    LocalDate effectiveDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
