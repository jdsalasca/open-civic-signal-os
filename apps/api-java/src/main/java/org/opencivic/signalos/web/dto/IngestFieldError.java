package org.opencivic.signalos.web.dto;

/**
 * One reason a single ingest row was rejected. Row-level errors are returned per row so a
 * data steward can fix the source file instead of bisecting a failed upload.
 */
public record IngestFieldError(
    int rowIndex,
    String field,
    String code,
    String message
) {}