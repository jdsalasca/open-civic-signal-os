package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityDecisionRepository extends JpaRepository<CommunityDecision, UUID> {
    List<CommunityDecision> findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(UUID communityId);
}
