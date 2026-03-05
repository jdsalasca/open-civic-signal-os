package org.opencivic.signalos.web.dto;

import java.util.List;

public record ExplainabilitySummary(
    String version,
    List<ExplainabilityFactor> topFactors,
    String summary
) {}
