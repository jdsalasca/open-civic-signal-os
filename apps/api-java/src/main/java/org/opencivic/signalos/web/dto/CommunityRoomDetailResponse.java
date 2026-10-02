package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityRoomDetailResponse(
    UUID id,
    UUID communityId,
    UUID projectBoardId,
    String name,
    String topic,
    UUID createdBy,
    LocalDateTime createdAt,
    boolean archived,
    boolean muted,
    LocalDateTime mutedAt,
    long messageCount,
    long unreadMentionCount,
    List<CommunityRoomMessageResponse> messages
) {}