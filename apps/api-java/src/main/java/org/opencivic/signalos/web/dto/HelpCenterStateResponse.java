package org.opencivic.signalos.web.dto;

import java.util.List;

public record HelpCenterStateResponse(
    List<String> completedStepKeys,
    List<String> dismissedGuideKeys
) {}
