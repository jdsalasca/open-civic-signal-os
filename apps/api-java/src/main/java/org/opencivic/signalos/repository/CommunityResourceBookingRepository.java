package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityResourceBooking;
import org.opencivic.signalos.domain.CommunityResourceBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityResourceBookingRepository extends JpaRepository<CommunityResourceBooking, UUID> {
    List<CommunityResourceBooking> findByCommunityIdOrderByRequestedAtDesc(UUID communityId);

    List<CommunityResourceBooking> findByRequesterIdOrderByRequestedAtDesc(UUID requesterId);

    /**
     * Half-open overlap: a booking blocks the window when it starts before the
     * requested end and ends after the requested start. Touching edges are fine,
     * so back-to-back bookings do not collide.
     */
    @Query("""
        select b from CommunityResourceBooking b
        where b.resourceId = :resourceId
          and b.status in :blockingStatuses
          and b.startsAt < :endsAt
          and b.endsAt > :startsAt
        """)
    List<CommunityResourceBooking> findOverlapping(
        @Param("resourceId") UUID resourceId,
        @Param("startsAt") java.time.LocalDateTime startsAt,
        @Param("endsAt") java.time.LocalDateTime endsAt,
        @Param("blockingStatuses") List<CommunityResourceBookingStatus> blockingStatuses
    );

    List<CommunityResourceBooking> findByStatusOrderByRequestedAtAsc(CommunityResourceBookingStatus status);
}