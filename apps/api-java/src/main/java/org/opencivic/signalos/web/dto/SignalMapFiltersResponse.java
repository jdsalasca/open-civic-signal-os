package org.opencivic.signalos.web.dto;

import java.time.LocalDate;
import java.util.List;

public record SignalMapFiltersResponse(
    String category,
    List<String> statuses,
    LocalDate fromDate,
    LocalDate toDate
) {
}
