package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityActivityBoardResponse(
    UUID communityId,
    String communityName,
    int openActivities,
    int myUpcomingSignups,
    List<CommunityActivityResponse> activities
) {}