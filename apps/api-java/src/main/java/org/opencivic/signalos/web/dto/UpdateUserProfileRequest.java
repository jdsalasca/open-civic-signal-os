package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import org.opencivic.signalos.domain.ProfileVisibility;

public record UpdateUserProfileRequest(
    @Size(max = 80) String displayName,
    @Size(max = 40) String civicRole,
    @Size(max = 240) String bio,
    List<@Size(max = 80) String> affiliations,
    ProfileVisibility profileVisibility,
    ProfileVisibility affiliationVisibility
) {}
