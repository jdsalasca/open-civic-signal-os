package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OpenDataDecisionRecordResponse(
    UUID id,
    UUID communityId,
    UUID linkedProposalId,
    UUID governanceDocumentId,
    UUID projectBoardId,
    String title,
    String summary,
    String decisionType,
    String decisionStatus,
    String approvalBasisType,
    String approvalBasisSummary,
    LocalDateTime decidedAt,
    LocalDate effectiveDate,
    LocalDateTime updatedAt
) {}
