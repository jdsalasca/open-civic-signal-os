package org.opencivic.signalos.domain;

public record ScoreBreakdown(
    double urgency,
    double impact,
    double affectedPeople,
    double communityVotes
) {}
