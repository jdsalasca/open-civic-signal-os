package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunityHomeSignalResponse(
    UUID id,
    String title,
    String status,
    Double priorityScore
) {}
