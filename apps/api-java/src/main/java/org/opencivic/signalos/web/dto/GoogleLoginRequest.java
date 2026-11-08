package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;

/** The ID token Google Identity Services returns to the browser after the user consents. */
public record GoogleLoginRequest(@NotBlank String credential) {}
