package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityIntegrationDelivery;
import org.opencivic.signalos.domain.CommunityIntegrationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityIntegrationDeliveryRepository
    extends JpaRepository<CommunityIntegrationDelivery, UUID> {
    List<CommunityIntegrationDelivery> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    List<CommunityIntegrationDelivery> findTop50ByIntegrationIdOrderByCreatedAtDesc(UUID integrationId);
    List<CommunityIntegrationDelivery> findByStatusOrderByCreatedAtAsc(CommunityIntegrationDeliveryStatus status);
    long countByCommunityIdAndStatus(UUID communityId, CommunityIntegrationDeliveryStatus status);
}