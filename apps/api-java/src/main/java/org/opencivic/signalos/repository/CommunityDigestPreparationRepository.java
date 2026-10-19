package org.opencivic.signalos.repository;

import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityDigestPreparation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityDigestPreparationRepository
    extends JpaRepository<CommunityDigestPreparation, UUID> {

    Optional<CommunityDigestPreparation> findByCommunityIdAndWeekKey(UUID communityId, String weekKey);
}