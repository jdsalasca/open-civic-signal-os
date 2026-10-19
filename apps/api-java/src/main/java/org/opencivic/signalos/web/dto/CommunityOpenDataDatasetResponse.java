package org.opencivic.signalos.web.dto;

import java.util.List;

public record CommunityOpenDataDatasetResponse(
    String resource,
    String description,
    List<String> formats,
    String externalPath
) {}
