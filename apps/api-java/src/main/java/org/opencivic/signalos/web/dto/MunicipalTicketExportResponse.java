package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Municipal ticket export response, including the field map that documents every column.
 *
 * <p>The field map ships inside every export on purpose. A municipality should be able to read
 * what each column means from the response itself, rather than asking the platform owner and
 * discovering the answer changed last quarter.
 */
public record MunicipalTicketExportResponse(
    UUID communityId,
    List<MunicipalTicketFieldResponse> fieldMap,
    List<MunicipalTicketRecordResponse> tickets,
    List<MunicipalTicketUnmappedCategoryResponse> unmappedCategories,
    int exportedCount,
    int excludedCount,
    Map<String, String> categoryMap
) {}