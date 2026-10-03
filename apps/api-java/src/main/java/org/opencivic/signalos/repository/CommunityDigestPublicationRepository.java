package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityDigestPublication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityDigestPublicationRepository
    extends JpaRepository<CommunityDigestPublication, UUID> {

    List<CommunityDigestPublication> findByCommunityIdOrderByWeekKeyDesc(UUID communityId);

    Optional<CommunityDigestPublication> findByCommunityIdAndWeekKey(UUID communityId, String weekKey);
}