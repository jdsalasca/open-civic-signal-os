package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityTrustPulseResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityTrustPulseResponseRepository
    extends JpaRepository<CommunityTrustPulseResponse, UUID> {

    Optional<CommunityTrustPulseResponse> findByCommunityIdAndRespondentIdAndPeriodKey(
        UUID communityId, UUID respondentId, String periodKey);

    List<CommunityTrustPulseResponse> findByCommunityIdAndPeriodKey(UUID communityId, String periodKey);

    List<CommunityTrustPulseResponse> findByCommunityIdOrderByPeriodKeyDesc(UUID communityId);
}