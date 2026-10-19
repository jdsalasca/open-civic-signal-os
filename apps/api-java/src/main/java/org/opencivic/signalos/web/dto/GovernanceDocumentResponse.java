package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GovernanceDocumentResponse(
    UUID id,
    UUID communityId,
    UUID createdBy,
    String authorUsername,
    String title,
    String summary,
    String documentType,
    String visibility,
    List<String> tags,
    Integer currentVersionNumber,
    GovernanceDocumentVersionResponse currentVersion,
    List<GovernanceDocumentVersionResponse> versions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
