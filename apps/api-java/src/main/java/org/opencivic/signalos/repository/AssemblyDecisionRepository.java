package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.AssemblyDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssemblyDecisionRepository extends JpaRepository<AssemblyDecision, UUID> {
    List<AssemblyDecision> findByAssemblyIdOrderByRecordedAtAsc(UUID assemblyId);
}