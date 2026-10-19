package org.opencivic.signalos.web.dto;

/**
 * New and resolved counts per day, so a community can see whether it is keeping up
 * rather than only how much is outstanding.
 */
public record SignalAgingTrendPointResponse(
    String date,
    long created,
    long resolved
) {}