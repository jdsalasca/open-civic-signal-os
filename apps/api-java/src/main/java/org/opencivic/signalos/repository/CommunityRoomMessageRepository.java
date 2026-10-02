package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMessageRepository extends JpaRepository<CommunityRoomMessage, UUID> {
    List<CommunityRoomMessage> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    long countByRoomId(UUID roomId);
}