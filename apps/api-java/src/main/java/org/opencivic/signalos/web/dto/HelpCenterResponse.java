package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HelpCenterResponse(
    String persona,
    String language,
    String surface,
    String query,
    LocalDateTime generatedAt,
    List<String> completedStepKeys,
    List<String> dismissedGuideKeys,
    List<OnboardingStepResponse> onboardingSteps,
    List<HelpGuideResponse> guides
) {}
