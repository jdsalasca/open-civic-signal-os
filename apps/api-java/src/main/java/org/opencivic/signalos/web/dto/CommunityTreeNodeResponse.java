package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record CommunityTreeNodeResponse(
    UUID id,
    String name,
    String slug,
    String description,
    UUID parentCommunityId,
    List<CommunityTreeNodeResponse> children
) {}
