package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunityActivitySignupResponse(
    UUID activityId,
    UUID communityId,
    UUID signupId,
    String status,
    String message
) {}