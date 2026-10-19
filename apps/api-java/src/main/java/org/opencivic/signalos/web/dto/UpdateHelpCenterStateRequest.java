package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateHelpCenterStateRequest(
    @NotNull List<String> completedStepKeys,
    @NotNull List<String> dismissedGuideKeys
) {}
