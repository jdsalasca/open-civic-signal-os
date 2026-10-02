package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PostCommunityRoomMessageRequest(
    @NotNull UUID communityId,
    @NotNull UUID roomId,
    @NotBlank @Size(min = 1, max = 2000) String body
) {}