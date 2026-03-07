package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunityProposalVotingResponse(
    UUID proposalId,
    CommunityProposalVoteConfigResponse config,
    boolean openForVoting,
    boolean canCurrentUserVote,
    String blockedReason,
    CommunityProposalVoteRecordResponse currentUserVote,
    CommunityProposalVoteTallyResponse tally,
    CommunityProposalVoteAuditSummaryResponse auditSummary
) {}
