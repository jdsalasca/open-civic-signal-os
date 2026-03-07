package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;

public record CommunityProposalVoteConfigResponse(
    String voteMode,
    String resultVisibility,
    String eligibilityRule,
    LocalDateTime votingOpensAt,
    LocalDateTime votingClosesAt
) {}
