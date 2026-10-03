package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.ParticipatoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipatoryBudgetRepository extends JpaRepository<ParticipatoryBudget, UUID> {
    List<ParticipatoryBudget> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    Optional<ParticipatoryBudget> findByIdAndCommunityId(UUID id, UUID communityId);
}