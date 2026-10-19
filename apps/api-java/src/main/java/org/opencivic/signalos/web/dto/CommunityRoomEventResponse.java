package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityRoomEventResponse(
    String type,
    UUID roomId,
    UUID communityId,
    UUID messageId,
    UUID authorId,
    LocalDateTime createdAt
) {}