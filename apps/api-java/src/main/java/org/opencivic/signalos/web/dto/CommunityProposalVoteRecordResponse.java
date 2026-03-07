package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityProposalVoteRecordResponse(
    UUID voterId,
    String voterUsername,
    String membershipRole,
    boolean verifiedMember,
    String choice,
    Integer scoreValue,
    LocalDateTime castAt
) {}
