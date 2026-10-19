package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityResourceBoardResponse(
    UUID communityId,
    String communityName,
    int resourceCount,
    int pendingApprovals,
    List<CommunityResourceResponse> resources,
    List<CommunityResourceBookingResponse> myBookings,
    List<CommunityResourceBookingResponse> approvalsQueue
) {}