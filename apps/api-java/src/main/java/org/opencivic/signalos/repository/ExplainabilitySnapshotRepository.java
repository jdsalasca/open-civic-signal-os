package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.ExplainabilitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplainabilitySnapshotRepository extends JpaRepository<ExplainabilitySnapshot, UUID> {
    List<ExplainabilitySnapshot> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    Optional<ExplainabilitySnapshot> findByIdAndCommunityId(UUID id, UUID communityId);
}