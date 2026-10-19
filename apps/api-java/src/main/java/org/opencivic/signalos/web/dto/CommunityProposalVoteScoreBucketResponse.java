package org.opencivic.signalos.web.dto;

public record CommunityProposalVoteScoreBucketResponse(
    int score,
    long count
) {}
