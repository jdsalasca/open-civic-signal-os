package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.SignalStatusEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SignalStatusEntryRepository extends JpaRepository<SignalStatusEntry, UUID> {
    List<SignalStatusEntry> findBySignalIdOrderByCreatedAtDesc(UUID signalId);
    List<SignalStatusEntry> findBySignalIdInOrderByCreatedAtAsc(List<UUID> signalIds);

    /** Resolution events are read from the audit trail rather than a denormalized column. */
    List<SignalStatusEntry> findByStatusToInAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
        Collection<String> statuses,
        LocalDateTime since
    );
}
