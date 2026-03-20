package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityOpenDataAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityOpenDataAccessLogRepository extends JpaRepository<CommunityOpenDataAccessLog, UUID> {
    List<CommunityOpenDataAccessLog> findTop20ByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    long countByTokenIdAndCreatedAtAfter(UUID tokenId, LocalDateTime createdAt);

    Optional<CommunityOpenDataAccessLog> findTopByTokenIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID tokenId, LocalDateTime createdAt);
}
