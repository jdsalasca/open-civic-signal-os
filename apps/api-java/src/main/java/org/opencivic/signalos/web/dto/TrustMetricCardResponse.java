package org.opencivic.signalos.web.dto;

public record TrustMetricCardResponse(
    String key,
    String label,
    String value,
    String unit,
    String definition,
    String formula,
    String supportingText
) {}
