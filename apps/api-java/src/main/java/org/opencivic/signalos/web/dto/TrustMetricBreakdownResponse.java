package org.opencivic.signalos.web.dto;

import java.util.List;

public record TrustMetricBreakdownResponse(
    String key,
    String title,
    String description,
    List<TrustMetricBreakdownItemResponse> items
) {}
