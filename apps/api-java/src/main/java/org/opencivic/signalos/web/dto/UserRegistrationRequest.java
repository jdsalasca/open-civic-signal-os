package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * A registration that can also attach the person to their city.
 *
 * <p>The three community fields are optional so a registration without a city still works. When
 * {@code communityId} is present the person joins that city; otherwise a non-blank
 * {@code communityName} founds a new one and the registrant becomes its organizer.
 */
public record UserRegistrationRequest(
    @NotBlank @Size(min = 4, max = 50) String username,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Email String email,
    UUID communityId,
    @Size(max = 160) String communityName,
    @Size(max = 120) String communityCountry,
    @Size(max = 120) String communityRegion
) {}
