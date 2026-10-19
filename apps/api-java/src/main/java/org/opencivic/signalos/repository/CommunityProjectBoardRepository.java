package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProjectBoardRepository extends JpaRepository<CommunityProjectBoard, UUID> {
    List<CommunityProjectBoard> findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(UUID communityId);
}
