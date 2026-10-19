package org.opencivic.signalos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityThreadRepository extends JpaRepository<CommunityThread, UUID> {
    List<CommunityThread> findBySourceCommunityIdOrTargetCommunityIdOrderByUpdatedAtDesc(
        UUID sourceCommunityId,
        UUID targetCommunityId
    );
    Page<CommunityThread> findBySourceCommunityIdOrTargetCommunityIdOrderByUpdatedAtDesc(
        UUID sourceCommunityId,
        UUID targetCommunityId,
        Pageable pageable
    );

    @Query("""
        select t from CommunityThread t
        where (t.sourceCommunityId = :communityId or t.targetCommunityId = :communityId)
          and (
            :status is null
            or (:status = 'ACTIVE' and t.updatedAt >= :cutoff)
            or (:status = 'STALE' and t.updatedAt < :cutoff)
          )
        order by t.updatedAt desc
    """)
    Page<CommunityThread> findByCommunityAndStatus(
        @Param("communityId") UUID communityId,
        @Param("status") String status,
        @Param("cutoff") LocalDateTime cutoff,
        Pageable pageable
    );

    @Query("""
        select t from CommunityThread t
        where (t.sourceCommunityId = :communityId or t.targetCommunityId = :communityId)
          and (
            :status is null
            or (:status = 'ACTIVE' and t.updatedAt >= :cutoff)
            or (:status = 'STALE' and t.updatedAt < :cutoff)
          )
    """)
    List<CommunityThread> findAllByCommunityAndStatus(
        @Param("communityId") UUID communityId,
        @Param("status") String status,
        @Param("cutoff") LocalDateTime cutoff
    );
}
