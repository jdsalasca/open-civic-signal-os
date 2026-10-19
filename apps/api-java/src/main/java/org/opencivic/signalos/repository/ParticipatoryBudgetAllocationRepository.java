package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.ParticipatoryBudgetAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipatoryBudgetAllocationRepository
    extends JpaRepository<ParticipatoryBudgetAllocation, UUID> {

    List<ParticipatoryBudgetAllocation> findByBudgetIdOrderByCreatedAtAsc(UUID budgetId);

    Optional<ParticipatoryBudgetAllocation> findByBudgetIdAndProposalId(UUID budgetId, UUID proposalId);
}