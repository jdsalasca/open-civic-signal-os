package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommunityDecisionRequest(
    @NotNull UUID communityId,
    UUID linkedProposalId,
    UUID governanceDocumentId,
    UUID projectBoardId,
    @Size(max = 120) String executionOwnerUsername,
    @NotBlank @Size(min = 8, max = 180) String title,
    @NotBlank @Size(min = 20, max = 4000) String summary,
    @NotBlank String decisionType,
    @NotBlank String decisionStatus,
    @NotBlank String approvalBasisType,
    @NotBlank @Size(min = 8, max = 2000) String approvalBasisSummary,
    LocalDateTime decidedAt,
    LocalDate effectiveDate
) {}
