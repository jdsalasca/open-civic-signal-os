package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCommunityProjectBoardRequest(
    @NotNull UUID communityId,
    UUID linkedProposalId,
    @NotBlank @Size(min = 8, max = 160) String title,
    @NotBlank @Size(min = 20, max = 4000) String summary,
    LocalDate dueDate
) {}
