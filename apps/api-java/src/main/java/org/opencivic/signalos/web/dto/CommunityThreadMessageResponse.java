package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityThreadMessageResponse(
    UUID id,
    UUID threadId,
    UUID authorId,
    UUID sourceCommunityId,
    String content,
    boolean hidden,
    String moderationReason,
    UUID hiddenBy,
    LocalDateTime hiddenAt,
    LocalDateTime createdAt
) {}
