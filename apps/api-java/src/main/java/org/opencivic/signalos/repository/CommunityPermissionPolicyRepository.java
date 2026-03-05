package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionPolicy;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPermissionPolicyRepository extends JpaRepository<CommunityPermissionPolicy, UUID> {
    List<CommunityPermissionPolicy> findByCommunityIdOrderByScopeAsc(UUID communityId);

    Optional<CommunityPermissionPolicy> findByCommunityIdAndScope(UUID communityId, CommunityPermissionScope scope);
}
