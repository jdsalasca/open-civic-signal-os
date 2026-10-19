package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommunityPrivacyPolicyRequest(
    @NotBlank String openDataPolicy
) {}
