package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {
    Page<Signal> findByStatusNotIn(Collection<String> statuses, Pageable pageable);
    
    // P1-B: Efficient paginated query for moderation
    Page<Signal> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT s FROM Signal s WHERE s.status = :status ORDER BY s.priorityScore DESC")
    List<Signal> findTopSignalsByStatus(@Param("status") String status, Pageable pageable);

    Page<Signal> findByAuthorId(UUID authorId, Pageable pageable);
}
