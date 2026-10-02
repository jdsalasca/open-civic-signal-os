package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommunityRoomRequest(
    @NotNull UUID communityId,
    @NotBlank @Size(min = 3, max = 120) String name,
    @NotBlank @Size(min = 3, max = 280) String topic,
    UUID projectBoardId
) {}