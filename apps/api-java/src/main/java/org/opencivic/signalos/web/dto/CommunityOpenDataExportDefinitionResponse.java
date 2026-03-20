package org.opencivic.signalos.web.dto;

import java.util.List;

public record CommunityOpenDataExportDefinitionResponse(
    String exportType,
    String requiredScope,
    String description,
    List<String> availableFormats
) {}
