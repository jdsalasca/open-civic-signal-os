package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CommunityBreadcrumbItemResponse(
    UUID id,
    String name,
    String slug
) {}
