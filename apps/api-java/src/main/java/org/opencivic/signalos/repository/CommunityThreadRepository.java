package org.opencivic.signalos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityThread;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
