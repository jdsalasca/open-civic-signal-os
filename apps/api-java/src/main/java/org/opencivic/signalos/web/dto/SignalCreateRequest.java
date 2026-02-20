package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignalCreateRequest(
    @NotBlank(message = "Signal title is mandatory")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    String title,
    
    @NotBlank(message = "Intelligence context is required")
    @Size(min = 10, max = 2000, message = "Description must provide sufficient detail (min 10 chars)")
    String description,
    
    @NotBlank(message = "Civic category must be specified")
    String category,
    
    @Min(value = 1, message = "Urgency must be at least 1") 
    @Max(value = 5, message = "Urgency factor cannot exceed 5")
    int urgency,
    
    @Min(value = 1, message = "Social impact must be at least 1") 
    @Max(value = 5, message = "Social impact factor cannot exceed 5")
    int impact,
    
    @Min(value = 1, message = "Affected scale must be at least 1 citizen") 
    int affectedPeople
) {}
