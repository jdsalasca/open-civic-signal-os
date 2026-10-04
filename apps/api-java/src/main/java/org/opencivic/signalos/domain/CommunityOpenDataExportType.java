package org.opencivic.signalos.domain;

public enum CommunityOpenDataExportType {
    SIGNALS,
    PROPOSALS,
    VOTES,
    DECISIONS,
    METRICS,
    /**
     * The prioritized, still-unresolved backlog for one community, as a peer would consume it.
     *
     * <p>Appended rather than inserted: {@code CommunityOpenDataTokenScope} pairs with this enum by
     * ordinal, so inserting a value would silently repoint every later token scope at the wrong
     * export.
     */
    PRIORITIZED_BACKLOG
}
