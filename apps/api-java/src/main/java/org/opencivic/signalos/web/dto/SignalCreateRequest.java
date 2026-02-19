package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignalCreateRequest(
    @NotBlank 
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters.")
    String title,
    
    @NotBlank 
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters.")
    String description,
    
    @NotBlank 
    String category,
    
    @Min(1) @Max(5) 
    int urgency,
    
    @Min(1) @Max(5) 
    int impact,
    
    @Min(1) 
    int affectedPeople
) {}
