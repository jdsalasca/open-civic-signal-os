package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunitySanction;
import org.opencivic.signalos.domain.CommunitySanctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunitySanctionRepository extends JpaRepository<CommunitySanction, UUID> {
    @Query("""
        select sanction from CommunitySanction sanction
        where sanction.communityId = :communityId
          and sanction.targetUserId = :targetUserId
          and sanction.status = :status
          and sanction.startsAt <= :now
          and (sanction.endsAt is null or sanction.endsAt >= :now)
        order by sanction.createdAt desc
    """)
    List<CommunitySanction> findActiveSanctions(
        @Param("communityId") UUID communityId,
        @Param("targetUserId") UUID targetUserId,
        @Param("status") CommunitySanctionStatus status,
        @Param("now") LocalDateTime now
    );

    long countByCommunityIdAndStatus(UUID communityId, CommunitySanctionStatus status);
}