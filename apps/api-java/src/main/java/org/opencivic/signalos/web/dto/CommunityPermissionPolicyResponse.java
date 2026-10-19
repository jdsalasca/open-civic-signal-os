package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityPermissionPolicyResponse(
    UUID communityId,
    String scope,
    List<String> allowedRoles,
    UUID updatedBy,
    LocalDateTime updatedAt
) {}
