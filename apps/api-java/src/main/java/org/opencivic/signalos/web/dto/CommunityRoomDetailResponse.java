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
    /**
     * Whether {@code messages} is a truncated page rather than the room's whole history.
     *
     * <p>Present because capping the list silently would be its own lie: a consumer that renders "no
     * older messages" from a capped array is wrong, and nothing in the payload would say so.
     */
    boolean hasMoreMessages,
    long unreadMentionCount,
    List<CommunityRoomMessageResponse> messages
) {}