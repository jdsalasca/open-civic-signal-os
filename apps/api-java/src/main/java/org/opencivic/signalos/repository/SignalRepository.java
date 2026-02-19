package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {
    Page<Signal> findByStatusNotIn(Collection<String> statuses, Pageable pageable);
}
