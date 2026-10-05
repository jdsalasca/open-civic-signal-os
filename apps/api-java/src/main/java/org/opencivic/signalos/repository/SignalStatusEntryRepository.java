package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.SignalStatusEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SignalStatusEntryRepository extends JpaRepository<SignalStatusEntry, UUID> {
    /**
     * The timeline, newest first.
     *
     * <p>Ordered by insertion sequence rather than by {@code createdAt}: a timestamp without
     * fractional seconds ties for anything a person does in one click of the other, and the database
     * would then be free to return either event first.
     */
    @Query("select e from SignalStatusEntry e where e.signalId = :signalId "
        + "order by e.createdAt desc, coalesce(e.seq, 0) desc")
    List<SignalStatusEntry> findTimeline(@Param("signalId") UUID signalId);
    List<SignalStatusEntry> findBySignalIdInOrderByCreatedAtAsc(List<UUID> signalIds);

    /** Resolution events are read from the audit trail rather than a denormalized column. */
    List<SignalStatusEntry> findByStatusToInAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
        Collection<String> statuses,
        LocalDateTime since
    );
}
