package org.opencivic.signalos.domain;

public enum CommunityIntegrationEventType {
    OFFICIAL_ANNOUNCEMENT,
    ACTIVITY_SCHEDULED,
    RESOURCE_BOOKED,
    /**
     * A rendered weekly digest. Unlike the others, its payload is not built from an event request:
     * the digest body is composed and sealed by the digest service, and the transport only carries it.
     */
    WEEKLY_DIGEST
}