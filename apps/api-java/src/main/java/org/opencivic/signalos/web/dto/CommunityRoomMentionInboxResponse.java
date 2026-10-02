package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityRoomMentionInboxResponse(
    UUID communityId,
    long unreadTotal,
    List<CommunityRoomMentionResponse> items
) {}