package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OpenDataProposalRecordResponse(
    UUID id,
    UUID communityId,
    UUID relatedSignalId,
    String title,
    String status,
    String problemStatement,
    String proposedSolution,
    String estimatedCost,
    String beneficiariesSummary,
    List<String> supportingLinks,
    String voteMode,
    String voteVisibility,
    String voteEligibility,
    LocalDateTime votingOpensAt,
    LocalDateTime votingClosesAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
