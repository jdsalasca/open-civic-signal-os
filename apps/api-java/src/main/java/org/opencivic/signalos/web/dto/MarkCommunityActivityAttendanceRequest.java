package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record MarkCommunityActivityAttendanceRequest(
    UUID communityId,
    UUID activityId,
    UUID signupId,
    String attendanceStatus
) {}