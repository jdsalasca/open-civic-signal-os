package org.opencivic.signalos.web.dto;

/**
 * A category the community has not yet given a city service code.
 *
 * <p>Reported rather than silently defaulted, because a report filed against the wrong service
 * code reaches the wrong municipal department and then stops being chased.
 */
public record MunicipalTicketUnmappedCategoryResponse(
    String category,
    String platformLabel,
    int affectedReports,
    String guidance
) {}