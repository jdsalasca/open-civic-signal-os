package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CommunityPermissionPolicyRuleRequest(
    @NotNull String scope,
    @NotEmpty List<String> allowedRoles
) {}
