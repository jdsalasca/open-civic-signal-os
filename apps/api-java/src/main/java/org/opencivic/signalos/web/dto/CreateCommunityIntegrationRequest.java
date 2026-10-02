package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record CreateCommunityIntegrationRequest(
    UUID communityId,
    String channel,
    String name,
    String targetUri,
    String secret,
    Boolean autoRetry
) {}