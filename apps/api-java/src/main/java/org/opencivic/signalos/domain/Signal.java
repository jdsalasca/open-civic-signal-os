package org.opencivic.signalos.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Signal(
    UUID id,
    String title,
    String description,
    String category,
    int urgency,
    int impact,
    int affectedPeople,
    int communityVotes,
    double priorityScore,
    ScoreBreakdown scoreBreakdown,
    String status,
    LocalDateTime createdAt
) {
    public Signal withScore(double score, ScoreBreakdown breakdown) {
        return new Signal(id, title, description, category, urgency, impact, affectedPeople, communityVotes, score, breakdown, status, createdAt);
    }
}
