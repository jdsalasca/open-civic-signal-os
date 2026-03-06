package org.opencivic.signalos.web.dto;

public record CommunityProposalDeliberationCountsResponse(
    int pros,
    int cons,
    int questions,
    int evidence,
    int visibleEntries,
    int hiddenEntries
) {}
