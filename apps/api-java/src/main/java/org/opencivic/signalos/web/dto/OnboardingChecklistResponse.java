package org.opencivic.signalos.web.dto;

public record OnboardingChecklistResponse(
    boolean joinedCommunity,
    boolean contributed,
    boolean followingContent,
    boolean allComplete
) {}
