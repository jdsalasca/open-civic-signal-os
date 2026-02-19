package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {
    Page<Signal> findByStatusNotIn(Collection<String> statuses, Pageable pageable);
    
    // P1-10: Scalable DB-level sorting for top signals
    @Query("SELECT s FROM Signal s WHERE s.status = 'NEW' ORDER BY s.priorityScore DESC")
    List<Signal> findTopSignalsByStatus(String status, Pageable pageable);
}
