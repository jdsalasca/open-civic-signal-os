package org.opencivic.signalos.web.dto;

import java.util.List;

public record IngestReportResponse(
    String source,
    String fileName,
    String contentSha256,
    long totalRows,
    long acceptedRows,
    long rejectedRows,
    boolean committed,
    List<String> detectedColumns,
    List<IngestRowOutcome> outcomes,
    List<IngestFieldError> errors
) {}