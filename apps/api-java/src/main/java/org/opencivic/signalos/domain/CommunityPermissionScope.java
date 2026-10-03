package org.opencivic.signalos.domain;

import java.util.Set;

public enum CommunityPermissionScope {
    CAST_PROPOSAL_VOTE(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
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
    MANAGE_MODERATION_QUEUE(Set.of(
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_PRIVACY_SETTINGS(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_OPEN_DATA_EXPORTS(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_DECISION_LEDGER(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_GOVERNANCE_LIBRARY(Set.of(
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
    POST_ROOM_MESSAGE(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_ROOMS(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    JOIN_ACTIVITIES(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_ACTIVITIES(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    BOOK_RESOURCES(Set.of(
        CommunityRole.MEMBER,
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_RESOURCES(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    MANAGE_INTEGRATIONS(Set.of(
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
    )),
    IMPORT_SIGNALS(Set.of(
        CommunityRole.MODERATOR,
        CommunityRole.COORDINATOR,
        CommunityRole.PUBLIC_SERVANT_LIAISON
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
