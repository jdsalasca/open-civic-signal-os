package org.opencivic.signalos.web.dto;

public record ProfileAchievementResponse(
    String key,
    boolean earned,
    int currentProgress,
    int targetProgress
) {}
