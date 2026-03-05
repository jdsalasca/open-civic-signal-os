package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record CommunityThreadMessageResponse(
    UUID id,
    UUID threadId,
    UUID authorId,
    UUID sourceCommunityId,
    UUID parentMessageId,
    int depth,
    int directReplyCount,
    String content,
    boolean hidden,
    String moderationReason,
    UUID hiddenBy,
    LocalDateTime hiddenAt,
    LocalDateTime createdAt,
    Map<String, Integer> reactions,
    String viewerReaction
) {}
