package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityAssembly;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityAssemblyRepository extends JpaRepository<CommunityAssembly, UUID> {
    List<CommunityAssembly> findByCommunityIdOrderByScheduledForDesc(UUID communityId);
    Optional<CommunityAssembly> findByIdAndCommunityId(UUID id, UUID communityId);
}