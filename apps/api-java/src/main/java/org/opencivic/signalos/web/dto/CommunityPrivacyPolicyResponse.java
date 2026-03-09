package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityPrivacyPolicyResponse(
    UUID communityId,
    String communityName,
    String openDataPolicy,
    String updatedByUsername,
    LocalDateTime updatedAt,
    List<SensitiveDataAccessLogResponse> recentAccessLogs
) {}
