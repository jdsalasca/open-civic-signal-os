package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityRoomSummaryResponse(
    UUID id,
    UUID communityId,
    UUID projectBoardId,
    String name,
    String topic,
    UUID createdBy,
    LocalDateTime createdAt,
    boolean archived,
    long messageCount,
    long unreadMentionCount,
    boolean muted,
    LocalDateTime lastActivityAt
) {}