package org.opencivic.signalos.web.dto;

import java.util.List;

public record CommunityProposalVoteTallyResponse(
    boolean visible,
    String visibilityReason,
    long totalBallots,
    long distinctVoters,
    double turnoutPercentage,
    long forVotes,
    long againstVotes,
    Double averageScore,
    List<CommunityProposalVoteScoreBucketResponse> scoreDistribution
) {}
