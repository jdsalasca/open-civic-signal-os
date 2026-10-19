package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityIntegrationRepository extends JpaRepository<CommunityIntegration, UUID> {
    List<CommunityIntegration> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    List<CommunityIntegration> findByCommunityIdAndChannelAndEnabledTrue(UUID communityId, CommunityIntegrationChannel channel);
}