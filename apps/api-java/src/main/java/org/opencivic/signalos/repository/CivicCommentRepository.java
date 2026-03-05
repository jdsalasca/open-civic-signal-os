package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.CivicComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface CivicCommentRepository extends JpaRepository<CivicComment, UUID> {
    List<CivicComment> findByParentIdAndParentTypeOrderByCreatedAtAsc(UUID parentId, String parentType);

    interface ParentCommentCount {
        UUID getParentId();
        long getCommentCount();
    }

    @Query("""
        select c.parentId as parentId, count(c) as commentCount
        from CivicComment c
        where c.parentType = :parentType and c.parentId in :parentIds
        group by c.parentId
        """)
    List<ParentCommentCount> countByParentIdsAndParentType(
        @Param("parentIds") List<UUID> parentIds,
        @Param("parentType") String parentType
    );
}
