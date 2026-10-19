package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityRoomMentionResponse(
    UUID id,
    UUID roomId,
    String roomName,
    UUID messageId,
    String messagePreview,
    UUID mentionedUserId,
    UUID mentionedBy,
    String mentionedByName,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}