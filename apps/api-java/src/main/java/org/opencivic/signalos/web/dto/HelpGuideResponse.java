package org.opencivic.signalos.web.dto;

import java.util.List;

public record HelpGuideResponse(
    String id,
    String kind,
    String surface,
    String audience,
    String title,
    String summary,
    String body,
    List<String> tags,
    String actionLabel,
    String actionRoute,
    boolean dismissible,
    boolean dismissed
) {}
