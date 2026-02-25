package org.opencivic.signalos.web.dto;

import org.opencivic.signalos.domain.ScoreBreakdown;
import java.util.Map;
import java.util.UUID;

public record SignalResponse(
    UUID id,
    String title,
    String description,
    String imageUrl,
    String category,
    String status,
    double priorityScore,
    ScoreBreakdown scoreBreakdown,
    int communityVotes,
    Map<String, Integer> reactions,
    String viewerReaction,
    Double latitude,
    Double longitude
) {}
