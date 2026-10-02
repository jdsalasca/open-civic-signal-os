package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, UUID> {
    List<CommunityActivity> findByCommunityIdAndCancelledFalseOrderByStartsAtAsc(UUID communityId);
}