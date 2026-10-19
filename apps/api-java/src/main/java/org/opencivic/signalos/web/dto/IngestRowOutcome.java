package org.opencivic.signalos.web.dto;

import java.util.List;
import java.util.UUID;

public record IngestRowOutcome(
    int rowIndex,
    boolean valid,
    String preview,
    UUID signalId,
    String sourceRef,
    List<IngestFieldError> errors
) {}