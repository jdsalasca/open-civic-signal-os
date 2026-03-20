package org.opencivic.signalos.web.dto;

public record CreateCommunityOpenDataTokenResponse(
    CommunityOpenDataTokenResponse token,
    String plainToken
) {}
