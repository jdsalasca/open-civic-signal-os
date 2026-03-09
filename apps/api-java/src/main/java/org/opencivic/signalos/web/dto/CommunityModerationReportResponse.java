package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityModerationReportResponse(
    UUID id,
    UUID communityId,
    String targetType,
    UUID targetId,
    String targetPreview,
    UUID reporterUserId,
    String reporterUsername,
    UUID reportedUserId,
    String reportedUsername,
    String reasonCode,
    String details,
    String status,
    boolean contentHidden,
    boolean falsePositiveReviewRecommended,
    String resolutionReason,
    String resolvedByUsername,
    CommunitySanctionResponse sanction,
    List<CommunityModerationActionResponse> actionHistory,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}