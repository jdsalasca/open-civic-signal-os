package org.opencivic.signalos.exception;

import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.springframework.security.access.AccessDeniedException;

public class CommunityPermissionDeniedException extends AccessDeniedException {
    private final UUID communityId;
    private final CommunityPermissionScope scope;
    private final CommunityRole currentRole;
    private final Set<CommunityRole> allowedRoles;

    public CommunityPermissionDeniedException(
        UUID communityId,
        CommunityPermissionScope scope,
        CommunityRole currentRole,
        Set<CommunityRole> allowedRoles
    ) {
        super("Forbidden: community role %s cannot perform %s. Allowed roles: %s."
            .formatted(currentRole, scope.name(), allowedRoles));
        this.communityId = communityId;
        this.scope = scope;
        this.currentRole = currentRole;
        this.allowedRoles = Set.copyOf(allowedRoles);
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public CommunityPermissionScope getScope() {
        return scope;
    }

    public CommunityRole getCurrentRole() {
        return currentRole;
    }

    public Set<CommunityRole> getAllowedRoles() {
        return allowedRoles;
    }
}
