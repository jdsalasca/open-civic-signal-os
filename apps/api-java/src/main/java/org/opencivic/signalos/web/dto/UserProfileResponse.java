package org.opencivic.signalos.web.dto;

import java.util.List;
import org.opencivic.signalos.domain.InterfaceMode;
import org.opencivic.signalos.domain.ProfileVisibility;

public record UserProfileResponse(
    String username,
    String displayName,
    String email,
    boolean verified,
    String civicRole,
    String bio,
    List<String> affiliations,
    ProfileVisibility profileVisibility,
    ProfileVisibility affiliationVisibility,
    InterfaceMode interfaceMode,
    String avatarPreset,
    List<ProfileAchievementResponse> achievements,
    String viewerScope
) {}
