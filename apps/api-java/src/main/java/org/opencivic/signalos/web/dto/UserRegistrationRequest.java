package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
    @NotBlank @Size(min = 4, max = 50) String username,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Email String email
) {}
