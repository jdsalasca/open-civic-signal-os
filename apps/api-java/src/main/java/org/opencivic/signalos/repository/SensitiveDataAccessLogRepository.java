package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.SensitiveDataAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveDataAccessLogRepository extends JpaRepository<SensitiveDataAccessLog, UUID> {
    List<SensitiveDataAccessLog> findTop20ByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
    List<SensitiveDataAccessLog> findTop20ByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);
    List<SensitiveDataAccessLog> findTop20ByCommunityIdOrderByCreatedAtDesc(UUID communityId);
}
