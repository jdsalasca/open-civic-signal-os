package org.opencivic.signalos.repository;

import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMessageRepository extends JpaRepository<CommunityRoomMessage, UUID> {
    /**
     * Paged on purpose: there is no unpaged variant of this query to fall into by accident. Loading a
     * room's whole history to count it or to show the newest few is the same cost either way, and the
     * count is what every caller actually wanted.
     *
     * <p>Id breaks createdAt ties on purpose. Two messages in the same second would otherwise order
     * unpredictably, and with a limit that means a message can be skipped or shown twice between two
     * page requests.
     */
    Page<CommunityRoomMessage> findByRoomIdOrderByCreatedAtDescIdDesc(UUID roomId, Pageable pageable);

    long countByRoomId(UUID roomId);
}