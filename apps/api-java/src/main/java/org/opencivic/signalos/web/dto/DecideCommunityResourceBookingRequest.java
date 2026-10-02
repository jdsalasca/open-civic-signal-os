package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record DecideCommunityResourceBookingRequest(
    UUID communityId,
    UUID bookingId,
    Boolean approve,
    String decisionNote
) {}