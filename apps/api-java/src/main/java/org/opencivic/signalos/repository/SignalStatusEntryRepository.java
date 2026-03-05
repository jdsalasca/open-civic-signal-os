package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.SignalStatusEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SignalStatusEntryRepository extends JpaRepository<SignalStatusEntry, UUID> {
    List<SignalStatusEntry> findBySignalIdOrderByCreatedAtDesc(UUID signalId);
}
