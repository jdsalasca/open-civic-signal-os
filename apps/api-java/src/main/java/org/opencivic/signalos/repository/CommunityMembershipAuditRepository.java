package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembershipAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityMembershipAuditRepository extends JpaRepository<CommunityMembershipAudit, UUID> {
    List<CommunityMembershipAudit> findByCommunityIdOrderByChangedAtDesc(UUID communityId);
}
