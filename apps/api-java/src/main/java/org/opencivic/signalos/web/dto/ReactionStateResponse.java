package org.opencivic.signalos.web.dto;

import java.util.Map;

public record ReactionStateResponse(
    Map<String, Integer> reactions,
    String viewerReaction
) {}
