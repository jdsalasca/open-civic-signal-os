package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomRepository extends JpaRepository<CommunityRoom, UUID> {
    List<CommunityRoom> findByCommunityIdAndArchivedFalseOrderByCreatedAtDesc(UUID communityId);
    List<CommunityRoom> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
}