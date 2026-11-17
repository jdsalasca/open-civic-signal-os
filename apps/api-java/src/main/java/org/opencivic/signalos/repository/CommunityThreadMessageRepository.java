package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityThreadMessageRepository extends JpaRepository<CommunityThreadMessage, UUID> {
    List<CommunityThreadMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
    List<CommunityThreadMessage> findByThreadIdInOrderByThreadIdAscCreatedAtAsc(Collection<UUID> threadIds);

    @Query("""
        select m.threadId as threadId, count(m) as total
        from CommunityThreadMessage m
        where m.threadId in :threadIds
        group by m.threadId
    """)
    List<ThreadMessageCount> countByThreadIds(@Param("threadIds") Collection<UUID> threadIds);

    interface ThreadMessageCount {
        UUID getThreadId();
        long getTotal();
    }

    /**
     * Only the earliest message of each thread, for the feed summary. Loading every message of every
     * thread on the page and keeping the first one costs the whole history for a preview line.
     */
    @Query("""
        select m from CommunityThreadMessage m
        where m.threadId in :threadIds
          and not exists (
            select earlier.id from CommunityThreadMessage earlier
            where earlier.threadId = m.threadId
              and earlier.createdAt < m.createdAt
          )
    """)
    List<CommunityThreadMessage> findEarliestByThreadIds(@Param("threadIds") Collection<UUID> threadIds);
}
