package org.opencivic.signalos.web.dto;

public record FormulaWeightResponse(
    String factor,
    String input,
    String expression,
    double cap
) {}