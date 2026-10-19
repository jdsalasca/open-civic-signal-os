package org.opencivic.signalos.web.dto;

import java.time.LocalDate;

/**
 * A closed calendar month, expressed as explicit bounds.
 *
 * <p>The dashboard metrics use a rolling window because a live view should always reflect
 * "now". A published report cannot: "the last 30 days" produces different numbers every time
 * it is run, so the figures in a published document could not be reproduced or challenged.
 * A closed period is what makes the output citable.
 */
public record TransparencyPeriod(
    String key,
    LocalDate startDate,
    LocalDate endDate,
    TransparencyPeriod previous
) {
    public boolean contains(java.time.LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        var local = timestamp.toLocalDate();
        return !local.isBefore(startDate) && local.isBefore(endDate);
    }
}