package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityBacklogPublication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityBacklogPublicationRepository
    extends JpaRepository<CommunityBacklogPublication, UUID> {

    Optional<CommunityBacklogPublication> findByCommunityIdAndCurrentSlotIsNotNull(UUID communityId);

    List<CommunityBacklogPublication> findByCommunityIdOrderByPublishedAtDesc(UUID communityId);
}