package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.SignalScoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SignalScoreEntryRepository extends JpaRepository<SignalScoreEntry, UUID> {

    /**
     * The score a signal held at a moment in time.
     *
     * <p>Ordered newest first and capped at one so the caller cannot accidentally load a signal's whole
     * scoring history to read a single figure.
     */
    @Query("select e from SignalScoreEntry e where e.signalId = :signalId and e.recordedAt <= :at "
        + "order by e.recordedAt desc")
    List<SignalScoreEntry> findLatestAtOrBefore(
        @Param("signalId") UUID signalId,
        @Param("at") LocalDateTime at,
        org.springframework.data.domain.Pageable pageable);

    default Optional<SignalScoreEntry> findLatestAtOrBefore(UUID signalId, LocalDateTime at) {
        return findLatestAtOrBefore(signalId, at, org.springframework.data.domain.Pageable.ofSize(1))
            .stream()
            .findFirst();
    }

    List<SignalScoreEntry> findBySignalIdOrderByRecordedAtDesc(UUID signalId);
}