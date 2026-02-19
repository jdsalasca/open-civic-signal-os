package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.*;

public record SignalCreateRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    @NotBlank(message = "Category is required")
    String category,

    @Min(1) @Max(5)
    int urgency,

    @Min(1) @Max(5)
    int impact,

    @Min(0)
    int affectedPeople
) {}
