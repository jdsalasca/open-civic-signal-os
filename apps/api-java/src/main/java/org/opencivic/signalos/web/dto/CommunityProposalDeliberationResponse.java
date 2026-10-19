package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityProposalDeliberationResponse(
    UUID proposalId,
    CommunityProposalDeliberationCountsResponse counts,
    List<CommunityProposalDeliberationEntryResponse> entries
) {}
