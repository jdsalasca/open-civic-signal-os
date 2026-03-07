package org.opencivic.signalos.web.dto;

public record TrustMetricBreakdownItemResponse(
    String label,
    long value,
    double share
) {}
