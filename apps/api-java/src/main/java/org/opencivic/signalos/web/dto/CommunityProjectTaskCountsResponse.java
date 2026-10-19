package org.opencivic.signalos.web.dto;

public record CommunityProjectTaskCountsResponse(
    int todo,
    int inProgress,
    int done
) {}
