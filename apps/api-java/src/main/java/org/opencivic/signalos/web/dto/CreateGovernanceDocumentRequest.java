package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateGovernanceDocumentRequest(
    @NotNull UUID communityId,
    @NotBlank @Size(min = 8, max = 180) String title,
    @NotBlank @Size(min = 20, max = 1200) String summary,
    @NotBlank String documentType,
    @NotBlank String visibility,
    @NotNull List<@Size(min = 2, max = 120) String> tags,
    @NotBlank @Size(min = 40, max = 12000) String content,
    @NotBlank @Size(min = 8, max = 500) String changeSummary,
    @Size(max = 1200) String sourceUrl,
    LocalDate effectiveDate,
    LocalDate meetingDate
) {}
