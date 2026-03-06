package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GovernanceDocumentVersionResponse(
    UUID id,
    UUID documentId,
    UUID createdBy,
    String authorUsername,
    Integer versionNumber,
    String content,
    String changeSummary,
    String sourceUrl,
    LocalDate effectiveDate,
    LocalDate meetingDate,
    LocalDateTime createdAt
) {}
