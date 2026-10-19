package org.opencivic.signalos.web.dto;

public record OnboardingStepResponse(
    String key,
    String audience,
    String title,
    String description,
    String actionLabel,
    String actionRoute,
    boolean completed,
    boolean dismissed
) {}
