package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMessageRepository extends JpaRepository<CommunityRoomMessage, UUID> {
    /**
     * Paged on purpose: there is no unpaged variant of this query to fall into by accident. Loading a
     * room's whole history to count it or to show the newest few is the same cost either way, and the
     * count is what every caller actually wanted.
     *
     * <p>Returns a List rather than a Page on purpose too. A {@code Page} makes Spring Data run a count
     * query to fill in the total, and the only caller that wants a total asks {@link #countByRoomId}
     * itself - so the count ran twice per room open, and the probe fired only when the page happened to
     * be full, which made the query count depend on the size of the page for no reason.
     *
     * <p>Id breaks createdAt ties on purpose. Two messages in the same second would otherwise order
     * unpredictably, and with a limit that means a message can be skipped or shown twice between two
     * page requests.
     */
    List<CommunityRoomMessage> findByRoomIdOrderByCreatedAtDescIdDesc(UUID roomId, Pageable pageable);

    long countByRoomId(UUID roomId);
}