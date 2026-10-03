package org.opencivic.signalos.web.dto;

import java.util.List;

/**
 * One free-text field in a public dataset and whether it currently carries PII.
 *
 * <p>Checklist entries are per field, not per dataset, because "the proposals dataset has
 * unresolved findings" is not actionable. "proposedSolution has 3 records mentioning an email
 * address" tells a data steward exactly which text to fix.
 */
public record PublicDataAnonymizationFieldCheck(
    String exportType,
    String field,
    long recordsScanned,
    long recordsWithFindings,
    List<String> categories,
    List<String> exampleFindings
) {
    /**
     * Serialized explicitly: a derived accessor on a record is not picked up by Jackson, and
     * this is the field a client reads to decide whether a gate is open.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("clean")
    public boolean clean() {
        return recordsWithFindings == 0;
    }
}