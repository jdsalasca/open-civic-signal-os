package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityRoomMessageResponse(
    UUID id,
    UUID roomId,
    UUID authorId,
    String authorName,
    String body,
    LocalDateTime createdAt,
    List<UUID> mentionedUserIds,
    boolean mentionsCurrentUser
) {}