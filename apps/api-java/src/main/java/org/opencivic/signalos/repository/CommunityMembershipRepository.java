package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {
    List<CommunityMembership> findByUserId(UUID userId);
    List<CommunityMembership> findByCommunityId(UUID communityId);
    Optional<CommunityMembership> findByUserIdAndCommunityId(UUID userId, UUID communityId);
}
