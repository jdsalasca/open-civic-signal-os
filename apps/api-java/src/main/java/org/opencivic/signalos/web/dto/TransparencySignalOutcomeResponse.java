package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single signal named in the report, in either the actioned or unaddressed list.
 *
 * <p>Unaddressed items are named on purpose. A transparency report that only lists wins is
 * marketing; the open items are the part a resident uses to judge whether the institution is
 * keeping up.
 */
public record TransparencySignalOutcomeResponse(
    UUID signalId,
    String title,
    String status,
    String category,
    double priorityScore,
    String locationLabel,
    int daysOpen,
    LocalDateTime reportedAt,
    LocalDateTime resolvedAt
) {}