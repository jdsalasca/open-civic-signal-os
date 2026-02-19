package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {
}
