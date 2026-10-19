package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityRoomMuteResponse(
    UUID roomId,
    UUID communityId,
    boolean muted,
    LocalDateTime mutedAt
) {}