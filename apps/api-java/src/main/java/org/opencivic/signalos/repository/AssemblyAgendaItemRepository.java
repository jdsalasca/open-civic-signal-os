package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.AssemblyAgendaItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssemblyAgendaItemRepository extends JpaRepository<AssemblyAgendaItem, UUID> {
    List<AssemblyAgendaItem> findByAssemblyIdOrderByPositionAsc(UUID assemblyId);
    void deleteByAssemblyId(UUID assemblyId);
}