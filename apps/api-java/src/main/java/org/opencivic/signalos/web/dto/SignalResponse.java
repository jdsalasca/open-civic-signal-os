package org.opencivic.signalos.web.dto;

import org.opencivic.signalos.domain.ScoreBreakdown;
import java.util.UUID;

public record SignalResponse(
    UUID id,
    String title,
    String description,
    String category,
    String status,
    double priorityScore,
    ScoreBreakdown scoreBreakdown
) {}
