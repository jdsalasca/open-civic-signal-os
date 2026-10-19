package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignSignalRequest(
    @NotBlank(message = "Assignee username is mandatory")
    String assigneeUsername,

    @Size(max = 400, message = "Assignment note must be at most 400 characters")
    String reason
) {}
