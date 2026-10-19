package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of a community's prioritized backlog, as served to a peer instance.
 *
 * <p>Deliberately carries the formula's <em>inputs</em> and the published expression rather than a
 * sentence explaining the rank. The digest renders a "why" in this platform's voice; a federated peer
 * consumes the same numbers and can render that sentence in its own language, and a second prose
 * version here would be a second thing to keep in step with the formula.
 */
public record OpenDataBacklogRecordResponse(
    /** The city's stable cross-instance key. What a peer addresses. */
    String cityKey,
    String cityName,
    /** Instance-local. Stable only within this deployment, so the city key is the real namespace. */
    UUID signalId,
    int rank,
    String title,
    String category,
    String status,
    double priorityScore,
    int urgency,
    int impact,
    int affectedPeople,
    int communityVotes,
    String locationLabel,
    String formulaVersion,
    String formulaExpression,
    LocalDateTime generatedAt
) {}