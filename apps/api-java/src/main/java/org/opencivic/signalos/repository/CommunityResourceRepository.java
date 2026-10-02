package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityResourceRepository extends JpaRepository<CommunityResource, UUID> {
    List<CommunityResource> findByCommunityIdAndArchivedFalseOrderByCreatedAtDesc(UUID communityId);
}