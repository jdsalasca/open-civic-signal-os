package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityAlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityAlertSubscriptionRepository extends JpaRepository<CommunityAlertSubscription, UUID> {
    Optional<CommunityAlertSubscription> findByUserIdAndCommunityId(UUID userId, UUID communityId);

    List<CommunityAlertSubscription> findByCommunityId(UUID communityId);

    void deleteByUserIdAndCommunityId(UUID userId, UUID communityId);
}
