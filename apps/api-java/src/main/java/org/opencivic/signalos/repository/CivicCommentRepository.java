package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.CivicComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CivicCommentRepository extends JpaRepository<CivicComment, UUID> {
    List<CivicComment> findByParentIdAndParentTypeOrderByCreatedAtAsc(UUID parentId, String parentType);
}
