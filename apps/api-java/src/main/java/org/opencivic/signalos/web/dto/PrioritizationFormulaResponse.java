package org.opencivic.signalos.web.dto;

import java.util.List;

/**
 * The scoring rule as data, so the formula is discoverable instead of buried in a string.
 * Weights are the exact multipliers the backend applies; cappedFactors lists the terms
 * with a ceiling and must stay in sync with PrioritizationServiceImpl.getBreakdown.
 */
public record PrioritizationFormulaResponse(
    String version,
    String formula,
    String effectiveFrom,
    List<FormulaWeightResponse> weights,
    List<String> cappedFactors,
    String changeNote
) {}