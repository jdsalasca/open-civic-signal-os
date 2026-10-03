package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.SignalMergeDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalMergeDecisionRepository extends JpaRepository<SignalMergeDecision, UUID> {
    List<SignalMergeDecision> findByCommunityIdOrderByDecidedAtDesc(UUID communityId);

    List<SignalMergeDecision> findByTargetSignalIdOrderByDecidedAtDesc(UUID targetSignalId);
}