package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpenDataVoteRecordResponse(
    UUID id,
    UUID communityId,
    UUID proposalId,
    String proposalTitle,
    String voteMode,
    String choice,
    Integer scoreValue,
    String membershipRole,
    boolean verifiedMember,
    LocalDateTime createdAt
) {}
