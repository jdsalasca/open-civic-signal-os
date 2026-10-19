package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateGovernanceDocumentVersionRequest(
    @NotBlank @Size(min = 40, max = 12000) String content,
    @NotBlank @Size(min = 8, max = 500) String changeSummary,
    @Size(max = 1200) String sourceUrl,
    LocalDate effectiveDate,
    LocalDate meetingDate
) {}
