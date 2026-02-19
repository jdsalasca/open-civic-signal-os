package org.opencivic.signalos.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Notification(
    UUID id,
    String channel,
    String message,
    String recipientGroup,
    LocalDateTime sentAt
) {}
