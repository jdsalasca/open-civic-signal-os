package org.opencivic.signalos.web.dto;

public record CommunityProposalVoteAuditSummaryResponse(
    long acceptedVotes,
    long duplicateBlockedAttempts,
    long eligibilityBlockedAttempts,
    long closedWindowBlockedAttempts
) {}
