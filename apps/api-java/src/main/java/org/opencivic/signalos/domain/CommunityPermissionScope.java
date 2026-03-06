package org.opencivic.signalos.domain;

import java.util.Set;

public enum CommunityPermissionScope {
    CREATE_PROPOSAL(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_PROJECT_BOARDS(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    CREATE_THREAD(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    ADD_THREAD_MESSAGE(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MODERATE_THREAD_MESSAGE(Set.of(
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR
    )),
    CREATE_OFFICIAL_UPDATE(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    UPDATE_OFFICIAL_UPDATE(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_MEMBERSHIPS(Set.of(
        CommunityRole.COORDINATOR
    )),
    MANAGE_PERMISSION_POLICIES(Set.of(
        CommunityRole.COORDINATOR
    )),
    VIEW_SENSITIVE_DATA(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    ));

    private final Set<CommunityRole> defaultAllowedRoles;

    CommunityPermissionScope(Set<CommunityRole> defaultAllowedRoles) {
        this.defaultAllowedRoles = Set.copyOf(defaultAllowedRoles);
    }

    public Set<CommunityRole> defaultAllowedRoles() {
        return defaultAllowedRoles;
    }
}
