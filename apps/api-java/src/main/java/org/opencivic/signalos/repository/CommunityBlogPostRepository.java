package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityBlogPostRepository extends JpaRepository<CommunityBlogPost, UUID> {
    @Query("""
        select p from CommunityBlogPost p
        where p.communityId = :communityId
          and p.archivedAt is null
        order by p.pinned desc, p.publishedAt desc
    """)
    List<CommunityBlogPost> findActiveOfficialTimeline(@Param("communityId") UUID communityId);

    @Query("""
        select p from CommunityBlogPost p
        where p.communityId = :communityId
          and p.archivedAt is not null
          and (:query is null
               or lower(p.title) like lower(concat('%', :query, '%'))
               or lower(p.content) like lower(concat('%', :query, '%')))
          and (:dateFrom is null or p.archivedAt >= :dateFrom)
          and (:dateTo is null or p.archivedAt <= :dateTo)
        order by p.archivedAt desc, p.publishedAt desc
    """)
    List<CommunityBlogPost> findArchivedOfficialTimeline(
        @Param("communityId") UUID communityId,
        @Param("query") String query,
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo") LocalDateTime dateTo
    );
}
