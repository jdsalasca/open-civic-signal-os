package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateCommunityProjectTaskRequest(
    @NotBlank @Size(min = 5, max = 160) String title,
    @NotBlank @Size(min = 10, max = 2000) String details,
    @NotBlank String status,
    String assigneeUsername,
    LocalDate dueDate
) {}
