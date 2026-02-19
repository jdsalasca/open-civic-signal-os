package org.opencivic.signalos.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record ScoreBreakdown(
    double urgency,
    double impact,
    double affectedPeople,
    double communityVotes
) {
    public ScoreBreakdown() {
        this(0, 0, 0, 0);
    }
}
