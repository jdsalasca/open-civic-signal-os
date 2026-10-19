package org.opencivic.signalos.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunitySanctionType;
import org.springframework.security.access.AccessDeniedException;

public class CommunitySanctionActiveException extends AccessDeniedException {
    private final UUID communityId;
    private final CommunitySanctionType sanctionType;
    private final LocalDateTime endsAt;

    public CommunitySanctionActiveException(UUID communityId, CommunitySanctionType sanctionType, LocalDateTime endsAt, String message) {
        super(message);
        this.communityId = communityId;
        this.sanctionType = sanctionType;
        this.endsAt = endsAt;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public CommunitySanctionType getSanctionType() {
        return sanctionType;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }
}