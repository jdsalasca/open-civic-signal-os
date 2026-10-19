package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMuteRepository extends JpaRepository<CommunityRoomMute, UUID> {
    Optional<CommunityRoomMute> findByRoomIdAndUserId(UUID roomId, UUID userId);
    List<CommunityRoomMute> findByRoomIdInAndUserId(List<UUID> roomIds, UUID userId);
}