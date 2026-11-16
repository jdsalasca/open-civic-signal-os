package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record SignalConfirmationStateResponse(
    UUID signalId,
    long count,
    boolean confirmedByViewer
) {}
