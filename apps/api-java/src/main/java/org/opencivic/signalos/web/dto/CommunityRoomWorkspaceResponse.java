package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityRoomWorkspaceResponse(
    UUID communityId,
    String communityName,
    List<String> mentionableUsernames,
    List<CommunityRoomSummaryResponse> rooms,
    CommunityRoomMentionInboxResponse mentionInbox
) {}